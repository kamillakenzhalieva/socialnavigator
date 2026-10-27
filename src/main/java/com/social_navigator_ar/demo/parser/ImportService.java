package com.social_navigator_ar.demo.parser;

import com.social_navigator_ar.demo.model.ImportRun;
import com.social_navigator_ar.demo.model.ImportedVacancy;
import com.social_navigator_ar.demo.model.ImportedVacancy.Status;
import com.social_navigator_ar.demo.repository.ImportRunRepository;
import com.social_navigator_ar.demo.repository.ImportedVacancyRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final ParserProperties props;
    private final ProfiScraper profi;
    private final YandexGptClient gpt;
    private final ImportedVacancyRepository vacancies;
    private final ImportRunRepository runs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "import-parser");
        t.setDaemon(true);
        return t;
    });

    public ImportService(ParserProperties props, ProfiScraper profi, YandexGptClient gpt,
                         ImportedVacancyRepository vacancies, ImportRunRepository runs) {
        this.props = props;
        this.profi = profi;
        this.gpt = gpt;
        this.vacancies = vacancies;
        this.runs = runs;
    }

    public boolean isRunning() { return running.get(); }

    public boolean startAsync(String trigger) {
        if (!running.compareAndSet(false, true)) return false;
        executor.submit(() -> {
            try {
                execute(trigger);
            } finally {
                running.set(false);
            }
        });
        return true;
    }


    private void execute(String trigger) {
        ImportRun run = new ImportRun();
        run.setTriggerType(trigger);
        run = runs.save(run);
        List<String> notes = new ArrayList<>();

        try {
            List<Candidate> candidates = new ArrayList<>();

            if (props.getProfi().isEnabled()) {
                ProfiScraper.Result r = profi.scrape(id -> vacancies.existsBySourceAndExternalId("PROFI", id));
                candidates.addAll(r.candidates());
                notes.addAll(r.warnings());

                for (String rejectedId : r.rejectedIds()) {
                    if (vacancies.existsBySourceAndExternalId("PROFI", rejectedId)) continue;
                    ImportedVacancy rej = new ImportedVacancy();
                    rej.setSource("PROFI");
                    rej.setExternalId(rejectedId);
                    rej.setTitle("(отклонён при проверке профиля)");
                    rej.setStatus(Status.AUTO_REJECTED);
                    try { vacancies.save(rej); } catch (DataIntegrityViolationException ignored) { }
                }
            }

            run.setFetched(candidates.size());
            int aiCalls = 0;

            for (Candidate c : candidates) {
                if (vacancies.existsBySourceAndExternalId(c.source(), c.externalId())) {
                    run.setDuplicates(run.getDuplicates() + 1);
                    continue;
                }

                ImportedVacancy v = new ImportedVacancy();
                v.setSource(c.source());
                v.setExternalId(c.externalId());
                v.setTitle(cut(c.title(), 290));
                v.setPersonName(cut(c.personName(), 290));
                v.setCity(cut(c.city(), 190));
                v.setSnippet(cut(c.text(), 1400));
                v.setSourceUrl(cut(c.url(), 990));
                v.setSection(cut(c.section(), 55));

                String text = c.text() == null ? "" : c.text();
                String head = text.length() > 300 ? text.substring(0, 300) : text;
                String headline = "PROFI".equals(c.source()) ? head : c.title() + " " + head;
                ParserCriteria.RuleResult rr = text.trim().length() < 40
                        ? new ParserCriteria.RuleResult(null, 0, 0, false, "слишком мало текста")
                        : ParserCriteria.evaluate(headline, c.title() + " " + text, c.section());
                int score = rr.score();
                String category = rr.category();
                String summary = null;
                String aiTitle = null;
                boolean relevant = score > 0;

                if (relevant && gpt.isAvailable() && aiCalls < props.getYandex().getMaxCallsPerRun()) {
                    try {
                        aiCalls++;
                        YandexGptClient.Verdict verdict = gpt.evaluate(c);
                        if (rr.autoPass()) {
                            relevant = true;
                            score = Math.max(score, verdict.score());
                        } else {
                            relevant = verdict.relevant();
                            score = verdict.score();
                            if (verdict.category() != null && !verdict.category().equals("null")) {
                                category = verdict.category();
                            }
                        }
                        summary = verdict.summary();
                        aiTitle = verdict.title();
                    } catch (Exception e) {
                        notes.add("ИИ: " + e.getMessage());
                        log.warn("YandexGPT: {}", e.getMessage());
                        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                    }
                }

                v.setCategory(category);
                v.setRelevance(score);
                v.setAiSummary(summary);
                if (aiTitle != null && !aiTitle.isBlank()) v.setTitle(cut(aiTitle, 290));
                boolean show = relevant && score >= props.getMinScore();
                v.setStatus(show ? Status.NEW : Status.AUTO_REJECTED);
                if (!show) {
                    String why = rr.reason() != null ? rr.reason()
                            : (!relevant ? "ИИ: не подходит (оценка " + score + ")"
                                         : "оценка " + score + " ниже порога " + props.getMinScore());
                    v.setRejectReason(cut(why, 190));
                }

                try {
                    vacancies.save(v);
                    if (show) run.setAdded(run.getAdded() + 1);
                    else run.setRejected(run.getRejected() + 1);
                } catch (DataIntegrityViolationException dup) {
                    run.setDuplicates(run.getDuplicates() + 1);
                }
            }

            run.setStatus("OK");
        } catch (Exception e) {
            log.error("Ошибка парсера", e);
            run.setStatus("ERROR");
            notes.add("Сбой: " + e.getMessage());
        }

        run.setMessage(cut(String.join(" | ", notes), 990));
        run.setFinishedAt(LocalDateTime.now());
        runs.save(run);
    }

    @PostConstruct
    void seedDemo() {
        if (!props.isDemoMode() || vacancies.count() > 0) return;
        demo("psychological", "Нейропсихолог", "Анна К.", "Астрахань",
                "Детский нейропсихолог, работаю с детьми с ЗПР, РАС и СДВГ. Диагностика и коррекционные занятия, очно в Астрахани.",
                "Детский нейропсихолог: диагностика и коррекция при ЗПР, РАС, СДВГ; очные занятия в Астрахани.", 92, "https://profi.ru/example/demo-1");
        demo("educational", "Логопед-дефектолог", "Мария С.", "Астрахань, Кировский р-н",
                "Постановка звуков, работа с алалией и дизартрией, занятия с детьми с ОВЗ и ДЦП.",
                "Логопед-дефектолог: речь при алалии, дизартрии, ДЦП; работает с детьми с ОВЗ.", 88, "https://profi.ru/example/demo-2");
        demo("legal", "Юрист по вопросам инвалидности", "Игорь П.", "Астрахань",
                "Помощь в оформлении инвалидности, обжалование решений МСЭ, консультации по соцвыплатам.",
                "Юрист: оформление инвалидности, споры с МСЭ, социальные выплаты.", 74, "https://profi.ru/example/demo-3");
        demo("social", "Тьютор-сопровождающий (вакансия)", "ГБОУ АО «Школа №0 (пример)»", "Астрахань",
                "Требуется тьютор для сопровождения учеников с ОВЗ в инклюзивных классах.",
                "Школа ищет тьютора для сопровождения учеников с ОВЗ в инклюзивных классах.", 81, "https://hh.ru/vacancy/0000000");
    }

    private void demo(String cat, String title, String name, String city, String text, String summary, int score, String url) {
        ImportedVacancy v = new ImportedVacancy();
        v.setSource("DEMO");
        v.setExternalId(url);
        v.setCategory(cat);
        v.setTitle(title);
        v.setPersonName(name);
        v.setCity(city);
        v.setSnippet(text);
        v.setAiSummary(summary);
        v.setRelevance(score);
        v.setSourceUrl(url);
        v.setStatus(Status.NEW);
        vacancies.save(v);
    }

    private static String cut(String s, int n) {
        if (s == null) return null;
        return s.length() <= n ? s : s.substring(0, n);
    }
}
