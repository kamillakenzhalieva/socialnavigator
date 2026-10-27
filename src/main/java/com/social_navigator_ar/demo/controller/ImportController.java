package com.social_navigator_ar.demo.controller;

import com.social_navigator_ar.demo.model.ImportRun;
import com.social_navigator_ar.demo.model.ImportedVacancy;
import com.social_navigator_ar.demo.model.ImportedVacancy.Status;
import com.social_navigator_ar.demo.model.Login;
import com.social_navigator_ar.demo.parser.ImportService;
import com.social_navigator_ar.demo.parser.ParserCriteria;
import com.social_navigator_ar.demo.parser.ParserProperties;
import com.social_navigator_ar.demo.repository.ImportRunRepository;
import com.social_navigator_ar.demo.repository.ImportedVacancyRepository;
import com.social_navigator_ar.demo.repository.LoginRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin/import")
@PreAuthorize("hasRole('ADMIN')")
public class ImportController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ImportService importService;
    private final ImportedVacancyRepository vacancies;
    private final ImportRunRepository runs;
    private final LoginRepository loginRepository;
    private final ParserProperties props;

    public ImportController(ImportService importService, ImportedVacancyRepository vacancies,
                            ImportRunRepository runs, LoginRepository loginRepository, ParserProperties props) {
        this.importService = importService;
        this.vacancies = vacancies;
        this.runs = runs;
        this.loginRepository = loginRepository;
        this.props = props;
    }

    @GetMapping
    public String page(Model model, Authentication authentication) {
        Login admin = loginRepository.findByEmail(authentication.getName()).orElse(null);
        model.addAttribute("adminUser", admin);
        model.addAttribute("isSuperAdmin", admin != null && admin.isSuperAdmin());

        List<ImportedVacancy> list = new ArrayList<>(
                vacancies.findByStatusNotInOrderByFoundAtDesc(List.of(Status.AUTO_REJECTED, Status.DISMISSED)));
        list.sort(Comparator.comparing((ImportedVacancy v) -> v.getStatus() != Status.NEW));
        model.addAttribute("vacancies", list);

        Map<String, String> categoryTitles = new LinkedHashMap<>();
        ParserCriteria.GROUPS.forEach(g -> categoryTitles.put(g.code(), g.title()));
        model.addAttribute("categoryTitles", categoryTitles);
        model.addAttribute("sourceTitles", Map.of("PROFI", "Профи.ру", "HH", "hh.ru", "DEMO", "Тест"));

        model.addAttribute("newCount", vacancies.countByStatus(Status.NEW));
        model.addAttribute("contactedCount", vacancies.countByStatus(Status.CONTACTED));
        model.addAttribute("lastRun", runs.findFirstByOrderByStartedAtDesc().orElse(null));
        model.addAttribute("running", importService.isRunning());
        model.addAttribute("demoMode", props.isDemoMode());
        model.addAttribute("aiConfigured", props.getYandex().isConfigured());
        model.addAttribute("profiEnabled", props.getProfi().isEnabled());
        model.addAttribute("hhEnabled", props.getHh().isEnabled());
        return "admin-import";
    }

    @PostMapping("/run")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> run() {
        boolean started = importService.startAsync("MANUAL");
        return ResponseEntity.ok(Map.of("started", started,
                "message", started ? "Парсер запущен" : "Парсер уже работает"));
    }

    @GetMapping("/status")
    @ResponseBody
    public Map<String, Object> status() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("running", importService.isRunning());
        res.put("newCount", vacancies.countByStatus(Status.NEW));
        ImportRun r = runs.findFirstByOrderByStartedAtDesc().orElse(null);
        if (r != null) {
            Map<String, Object> last = new LinkedHashMap<>();
            last.put("status", r.getStatus());
            last.put("startedAt", r.getStartedAt().format(FMT));
            last.put("added", r.getAdded());
            last.put("fetched", r.getFetched());
            last.put("message", r.getMessage());
            res.put("lastRun", last);
        }
        return res;
    }

    @PostMapping("/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changeStatus(@PathVariable Long id,
                                                            @RequestBody Map<String, String> body) {
        Status st;
        try {
            st = Status.valueOf(body.getOrDefault("status", ""));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Неизвестный статус"));
        }
        if (st == Status.AUTO_REJECTED) {
            return ResponseEntity.badRequest().body(Map.of("message", "Недопустимый статус"));
        }
        return vacancies.findById(id).map(v -> {
            v.setStatus(st);
            vacancies.save(v);
            return ResponseEntity.ok(Map.<String, Object>of("status", st.name()));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
