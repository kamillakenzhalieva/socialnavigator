package com.social_navigator_ar.demo.parser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProfiScraper {

    private static final Logger log = LoggerFactory.getLogger(ProfiScraper.class);

    public record Result(List<Candidate> candidates, List<String> warnings, List<String> rejectedIds) {
        public Result(List<Candidate> candidates, List<String> warnings) {
            this(candidates, warnings, List.of());
        }
    }

    private record Inspection(Candidate candidate, boolean definiteReject) { }

    private static final String[] CARD_SELECTORS = {
            "[data-shmid='profileCard']",
            "[data-testid='profileCard']",
            "[class*='ProfileCard']",
            "[class*='profileCard']",
            "article[class*='card']",
            "li[class*='card']"
    };


    private static final List<String> ASTRAKHAN_TERMS = List.of(
            "астрахан", "ахтубинск", "харабал", "знаменск", "камызяк", "икрянин", "икряное",
            "нариманов", "енотаевк", "началово", "черный яр", "красный яр", "мумра", "оранжерейн",
            "лиман", "володарск");

    private static final List<String> ASTRAKHAN_DISTRICTS = List.of(
            "кировск", "ленинск", "советск", "трусовск");

    private static final List<String> ON_SITE_MARKERS = List.of(
            "принимает у себя", "выезд к ученику", "выезд к клиенту", "выезд", "адрес", "показать на карте");

    private static final String REMOTE_MARKER = "дистанцион";

    private static final double LAT_MIN = 45.4, LAT_MAX = 48.9, LON_MIN = 45.0, LON_MAX = 49.8;
    private static final Pattern COORD = Pattern.compile("(\\d{2}\\.\\d{4,})\\D{1,6}(\\d{2}\\.\\d{4,})");
    private static final String COORD_OK = "[[COORD_OK]]";
    private static final Pattern PROFILE_ID = Pattern.compile("/profile/([A-Za-z0-9_-]{3,})");
    private static final Pattern CHOSEN_ID = Pattern.compile("[Pp]rofileId=([A-Za-z0-9_-]{3,})");
    private static final int MAX_LIST_ROUNDS = 60;

    private static final Pattern NAME = Pattern.compile(
            "^[А-ЯЁ][а-яё]+(?:-[А-ЯЁ][а-яё]+)?\\s+(?:[А-ЯЁ]\\.|[А-ЯЁ][а-яё]+(?:-[А-ЯЁ][а-яё]+)?)(?:\\s+[А-ЯЁ][а-яё]+)?$");

    private final ParserProperties props;

    public ProfiScraper(ParserProperties props) {
        this.props = props;
    }

    public Result scrape() {
        return scrape(id -> false);
    }

    public Result scrape(Predicate<String> alreadyKnown) {
        List<Candidate> out = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Stats st = new Stats();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setSlowMo(50.0)
                            .setArgs(List.of("--disable-blink-features=AutomationControlled")));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(props.getProfi().getUserAgent())
                    .setLocale("ru-RU")
                    .setViewportSize(1366, 900));

            context.addInitScript(
                    "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });"
                            + "Object.defineProperty(navigator, 'languages', { get: () => ['ru-RU','ru','en-US'] });"
                            + "Object.defineProperty(navigator, 'plugins', { get: () => [1,2,3,4,5] });"
                            + "window.chrome = { runtime: {} };");

            Page page = context.newPage();

            for (String root : props.getProfi().getRootUrls()) {
                if (!root.contains("geo-astrahan") && !root.contains("geo-astrakhan")) {
                    warnings.add("Профи.ру: пропущен адрес без geo-astrahan: " + root);
                    continue;
                }
                try {
                    collectFromList(page, root, out, rejected, seen, alreadyKnown, st);
                } catch (Exception e) {
                    warnings.add("Профи.ру: ошибка на " + root + " — " + e.getMessage());
                    log.warn("Profi scrape error на {}: {}", root, e.getMessage());
                }
                pause();
            }
            browser.close();
        } catch (Exception e) {
            warnings.add("Профи.ру: критическая ошибка Playwright — " + e.getMessage());
            log.error("Playwright fatal error", e);
        }

        log.info("Профи.ру: итог — открыто профилей {}, принято {}, отклонено {}, уже были в базе {}",
                st.opened, st.accepted, st.rejected, st.known);
        if (st.opened == 0 && st.known == 0 && out.isEmpty() && warnings.isEmpty()) {
            warnings.add("Профи.ру: не найдено ни одного профиля (подробности в логе и в папке profi-debug/).");
        }
        return new Result(out, warnings, rejected);
    }

    private static class Stats {
        int opened, accepted, rejected, known;
    }

    private void collectFromList(Page page, String root, List<Candidate> out, List<String> rejected,
                                 Set<String> seen, Predicate<String> alreadyKnown, Stats st) {
        String section = sectionOf(root);
        String tag = section.replaceAll("[^a-zA-Z0-9]+", "_");
        int[] rootOpened = {0};
        int o0 = st.opened, a0 = st.accepted, r0 = st.rejected, k0 = st.known;

        int found = processListPage(page, withProfilesTab(root), "list_" + tag, section,
                out, rejected, seen, alreadyKnown, st, true, rootOpened);

        if (found == 0) {
            log.info("Профи.ру [{}]: на странице-списке профилей нет — пробую обычную страницу раздела", section);
            found = processListPage(page, root.replaceAll("[?#].*$", ""), "landing_" + tag, section,
                    out, rejected, seen, alreadyKnown, st, false, rootOpened);
        }

        if (found == 0) {
            List<String> children = discoverChildSections(page, root);
            log.info("Профи.ру [{}]: найдено подразделов: {} {}", section, children.size(), children);
            for (String child : children) {
                String childTag = tag + "_" + sectionOf(child).replaceAll("[^a-zA-Z0-9]+", "_");
                processListPage(page, withProfilesTab(child), "list_" + childTag, section,
                        out, rejected, seen, alreadyKnown, st, true, rootOpened);
                if (rootOpened[0] >= props.getMaxProfilesPerRun()) break;
                pause();
            }
        }

        log.info("Профи.ру [{}]: итог раздела — открыто {}, принято {}, отклонено {}, уже были в базе {}",
                section, st.opened - o0, st.accepted - a0, st.rejected - r0, st.known - k0);
    }

    private List<String> discoverChildSections(Page page, String root) {
        String path = root.replaceAll("^https?://[^/]+", "").replaceAll("[?#].*$", "");
        if (!path.endsWith("/")) path += "/";
        Set<String> out = new LinkedHashSet<>();
        try {
            Object res = page.evaluate("() => Array.from(document.querySelectorAll('a[href]')).map(a => a.getAttribute('href'))");
            if (res instanceof List<?> list) {
                for (Object o : list) {
                    if (o == null) continue;
                    String h = o.toString().replaceAll("[?#].*$", "").replaceFirst("^https?://profi\\.ru", "");
                    if (h.startsWith(path) && h.length() > path.length()) {
                        String rest = h.substring(path.length()).replaceAll("/+$", "");
                        if (!rest.isEmpty() && !rest.contains("/")) out.add("https://profi.ru" + path + rest + "/");
                    }
                    if (out.size() >= 10) break;
                }
            }
        } catch (Exception e) {
            log.debug("Профи.ру: не удалось собрать подразделы: {}", e.getMessage());
        }
        return new ArrayList<>(out);
    }

    private int processListPage(Page page, String url, String tag, String section, List<Candidate> out,
                                List<String> rejected, Set<String> seen, Predicate<String> alreadyKnown,
                                Stats st, boolean paginate, int[] rootOpened) {
        log.info("Профи.ру: открываю {}", url);
        page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        dismissCookieBanner(page);
        boolean listed = waitForListing(page, paginate ? 20_000 : 5_000);
        if (!listed) log.info("Профи.ру [{}]: список специалистов не появился за отведённое время", section);

        int totalFound = 0;
        for (int round = 1; round <= (paginate ? MAX_LIST_ROUNDS : 1); round++) {
            autoScroll(page);
            List<String> urls = collectProfileUrls(page);
            totalFound = Math.max(totalFound, urls.size());

            if (round == 1 && urls.isEmpty()) {
                diagnose(page, tag);
                return 0;
            }

            int fresh = 0;
            for (String u : urls) {
                String externalId = stableId(u);
                if (!seen.add(externalId)) continue;
                fresh++;
                if (alreadyKnown.test(externalId)) { st.known++; continue; }
                if (rootOpened[0] >= props.getMaxProfilesPerRun()) {
                    log.info("Профи.ру [{}]: достигнут лимит {} НОВЫХ профилей на раздел — остальные в следующий запуск",
                            section, props.getMaxProfilesPerRun());
                    return totalFound;
                }
                rootOpened[0]++;
                st.opened++;

                Inspection ins = inspectProfile(page, u, section);
                if (ins.candidate() != null) {
                    out.add(ins.candidate());
                    st.accepted++;
                } else {
                    st.rejected++;
                    if (ins.definiteReject()) rejected.add(externalId);
                }
                pause();
            }
            log.info("Профи.ру [{}]: раунд {} — профилей на странице {}, новых для проверки {}",
                    section, round, urls.size(), fresh);

            if (!paginate) break;
            if (!clickShowMore(page)) {
                log.info("Профи.ру [{}]: список закончился (кнопки «Показать ещё» больше нет)", section);
                break;
            }
        }
        return totalFound;
    }

    private void diagnose(Page page, String tag) {
        try {
            String body = ParserCriteria.norm(page.innerText("body"));
            boolean botWall = body.contains("captcha") || body.contains("капча") || body.contains("не робот")
                    || body.contains("подозрительн") || body.contains("доступ ограничен")
                    || body.contains("access denied") || body.contains("qrator");
            log.warn("Профи.ру [диагностика {}]: URL={}; заголовок «{}»; признаки защиты от ботов: {}",
                    tag, page.url(), page.title(), botWall ? "ДА" : "нет");
            Path dir = Paths.get("profi-debug");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(tag + ".html"), page.content());
            page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(tag + ".png")).setFullPage(true));
        } catch (Exception e) {
            log.warn("Профи.ру: диагностика не удалась: {}", e.getMessage());
        }
    }

    private static String withProfilesTab(String url) {
        if (url.contains("tabName=")) return url;
        return url + (url.contains("?") ? "&" : "?") + "seamless=1&tabName=PROFILES";
    }

    private List<String> collectProfileUrls(Page page) {
        Set<String> ids = extractIds(page.content());
        List<String> urls = new ArrayList<>();
        for (String id : ids) urls.add("https://profi.ru/profile/" + id + "/");
        return urls;
    }

    private static Set<String> extractIds(String html) {
        Set<String> ids = new LinkedHashSet<>();
        if (html == null) return ids;
        for (Pattern p : new Pattern[]{PROFILE_ID, CHOSEN_ID}) {
            Matcher m = p.matcher(html);
            while (m.find()) ids.add(m.group(1));
        }
        return ids;
    }

    private Inspection inspectProfile(Page listPage, String url, String section) {
        Page p = null;
        try {
            p = listPage.context().newPage();
            p.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            p.waitForTimeout(1500);

            String body = p.innerText("body");
            if (body == null || body.length() < 200) {
                log.info("Профи.ру: страница не загрузилась (временно) — {}", url);
                return new Inspection(null, false);
            }

            java.util.Map<String, String> secs = parseSections(body);
            String about = collapse(secs.getOrDefault("О себе", section(body, "О себе", "Образование")));
            String edu = collapse(secs.getOrDefault("Образование", ""));
            String exp = collapse(secs.getOrDefault("Опыт", ""));
            String ach = collapse(secs.getOrDefault("Достижения", ""));
            String servicesRaw = secs.getOrDefault("Услуги и цены", "");
            String services = serviceNames(servicesRaw, 5);

            String qualLine = lineStartingWith(body, "Специалист подтвердил квалификацию");
            String qual = qualLine.replaceFirst("(?iu)^специалист подтвердил квалификацию по\\s*", "").trim();
            String audience = lineAfter(body, qualLine, "Может");
            String rating = extractRating(body);

            if (about.length() < 20 && qual.isBlank() && services.isBlank()) {
                log.info("Профи.ру: ОТКЛОНЁН (скрытое объявление / нет описания) — {}", url);
                return new Inspection(null, true);
            }

            String region = detectRegion(p, body);
            if (region == null) {
                log.info("Профи.ру: ОТКЛОНЁН (не Астрахань / дистанционно) — {}", url);
                return new Inspection(null, true);
            }

            StringBuilder sb = new StringBuilder();
            if (!qual.isBlank()) sb.append("Квалификация: ").append(qual).append('\n');
            if (!audience.isBlank()) sb.append(audience).append('\n');
            if (!rating.isBlank()) sb.append("Рейтинг: ").append(rating).append('\n');
            if (!about.isBlank()) sb.append("О себе: ").append(cut(about, 2500)).append('\n');
            if (!edu.isBlank()) sb.append("Образование: ").append(cut(edu, 1200)).append('\n');
            if (!exp.isBlank()) sb.append("Опыт: ").append(cut(exp, 800)).append('\n');
            if (!ach.isBlank()) sb.append("Достижения: ").append(cut(ach, 800)).append('\n');
            if (!services.isBlank()) sb.append("Услуги: ").append(services);
            String text = cut(sb.toString().trim(), 6500);

            String fullName = extractName(p, body);
            String name = fullName != null ? shortName(fullName) : fallbackName(p);

            String title = sectionLabel(section);

            log.info("Профи.ру: ПРИНЯТ '{}' ({}) — регион: {}", name, url, region);
            return new Inspection(new Candidate("PROFI", stableId(url), title, name, region, text,
                    stripQuery(url), section), false);
        } catch (Exception e) {
            log.warn("Профи.ру: не удалось разобрать профиль {}: {}", url, e.getMessage());
            return new Inspection(null, false);
        } finally {
            if (p != null) { try { p.close(); } catch (Exception ignored) { } }
        }
    }

    private static final List<String> HEADINGS = List.of(
            "О себе", "Образование", "Опыт", "Достижения", "Услуги и цены",
            "Портфолио", "Фото", "Видео", "Отзывы", "Похожие специалисты", "Похожие");

    private static java.util.Map<String, String> parseSections(String body) {
        java.util.Map<String, String> res = new java.util.LinkedHashMap<>();
        String current = null;
        StringBuilder sb = new StringBuilder();
        for (String line : body.split("\\R")) {
            String t = line.trim();
            if (HEADINGS.contains(t)) {
                if (current != null && !res.containsKey(current)) res.put(current, sb.toString().trim());
                sb.setLength(0);
                if (t.equals("Отзывы") || t.startsWith("Похожие")) { current = null; break; }
                current = res.containsKey(t) ? null : t;
                continue;
            }
            if (current != null) sb.append(line).append('\n');
        }
        if (current != null && !res.containsKey(current)) res.put(current, sb.toString().trim());
        return res;
    }

    private String fallbackName(Page p) {
        try {
            Locator h1 = p.locator("h1");
            if (h1.count() > 0) {
                String t = h1.first().innerText().trim();
                String n = ParserCriteria.norm(t);
                if (!t.isEmpty() && t.length() <= 60 && !n.contains("быстрый поиск") && !n.contains("профи")) return t;
            }
        } catch (Exception ignored) { }
        return "Специалист";
    }

    private String extractName(Page p, String body) {
        try {
            Locator h1 = p.locator("h1");
            if (h1.count() > 0) {
                String t = h1.first().innerText().trim();
                if (NAME.matcher(t).matches()) return t;
            }
        } catch (Exception ignored) { }
        int seen = 0;
        for (String l : body.split("\\R")) {
            String t = l.trim();
            if (t.isEmpty()) continue;
            if (NAME.matcher(t).matches()) return t;
            if (++seen > 15) break;
        }
        return null;
    }

    static String shortName(String full) {
        String[] t = full.trim().split("\\s+");
        if (t.length == 1) return full.trim();
        if (t[1].endsWith(".")) return t[0] + " " + t[1];
        return t[0] + " " + t[t.length - 1].charAt(0) + ".";
    }

    private String detectRegion(Page profile, String body) {
        String header = headerBlock(body);
        String headerN = ParserCriteria.norm(header);
        boolean remote = headerN.contains(REMOTE_MARKER);
        boolean onSite = ON_SITE_MARKERS.stream().anyMatch(headerN::contains);

        for (String raw : header.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.length() > 60) continue;
            String n = ParserCriteria.norm(line);
            if (ASTRAKHAN_TERMS.stream().anyMatch(n::contains)) return line;
            if (onSite && ASTRAKHAN_DISTRICTS.stream().anyMatch(n::contains)) return "Астрахань, " + line;
        }

        if (onSite) {
            String mapInfo = readMapInfo(profile);
            if (!mapInfo.isBlank()) {
                String m = ParserCriteria.norm(mapInfo);
                if (ASTRAKHAN_TERMS.stream().anyMatch(m::contains)) return "Астрахань (по карте)";
                if (mapInfo.contains(COORD_OK)) return "Астраханская область (по координатам карты)";
            }
        }

        if (!remote && !onSite) {
            String edu = ParserCriteria.norm(section(body, "Образование", "Опыт"));
            if (edu.contains("астрахан")) return "Астрахань (по вузу — слабый признак)";
        }
        return null;
    }

    private static String headerBlock(String body) {
        int a1 = body.indexOf("Связаться");
        int a2 = body.indexOf("Написать сообщение");
        int a = a1 < 0 ? a2 : (a2 < 0 ? a1 : Math.min(a1, a2));
        if (a < 0) a = 0;
        int b = body.indexOf("О себе", a);
        if (b < 0) b = Math.min(body.length(), a + 700);
        return body.substring(a, b);
    }

    private String readMapInfo(Page p) {
        try {
            Locator btn = p.locator("text=Показать на карте");
            if (btn.count() == 0) return "";
            btn.first().scrollIntoViewIfNeeded();
            btn.first().click();
            p.waitForTimeout(2000);

            StringBuilder sb = new StringBuilder();
            for (String t : p.locator("[role='dialog'], [class*='odal'], [class*='opup'], [class*='map'], [class*='Map']")
                    .allInnerTexts()) {
                sb.append(t).append(' ');
            }
            Matcher m = COORD.matcher(p.content());
            int guard = 0;
            while (m.find() && guard++ < 300) {
                double x = Double.parseDouble(m.group(1)), y = Double.parseDouble(m.group(2));
                if (inBox(x, y) || inBox(y, x)) { sb.append(' ').append(COORD_OK); break; }
            }
            try { p.keyboard().press("Escape"); } catch (Exception ignored) { }
            String res = sb.toString().trim();
            log.info("Профи.ру: данные с карты: {}", cut(res, 200));
            return res;
        } catch (Exception e) {
            log.debug("Профи.ру: не удалось прочитать карту: {}", e.getMessage());
            return "";
        }
    }

    private static boolean inBox(double lat, double lon) {
        return lat >= LAT_MIN && lat <= LAT_MAX && lon >= LON_MIN && lon <= LON_MAX;
    }

    private static String collapse(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private static String lineStartingWith(String body, String prefix) {
        for (String l : body.split("\\R")) {
            String t = l.trim();
            if (t.regionMatches(true, 0, prefix, 0, prefix.length())) return t;
        }
        return "";
    }

    private static String lineAfter(String body, String after, String prefix) {
        if (after == null || after.isBlank()) return "";
        String[] lines = body.split("\\R");
        for (int i = 0; i < lines.length - 1; i++) {
            if (lines[i].trim().equals(after)) {
                String next = lines[i + 1].trim();
                return next.startsWith(prefix) ? next : "";
            }
        }
        return "";
    }

    private static String extractRating(String body) {
        String rating = "", reviews = "";
        for (String l : body.split("\\R")) {
            String t = l.trim();
            if (rating.isEmpty() && t.matches("\\d[,.]\\d{1,2}")) rating = t;
            else if (reviews.isEmpty() && t.matches("(?iu)\\d+\\s+отзыв\\p{L}*")) reviews = t;
            if (!rating.isEmpty() && !reviews.isEmpty()) break;
        }
        if (rating.isEmpty() && reviews.isEmpty()) return "";
        return rating.isEmpty() ? reviews : (reviews.isEmpty() ? rating : rating + " · " + reviews);
    }

    private static String sectionLabel(String section) {
        return switch (section.toLowerCase()) {
            case "psihologia", "psiholog", "psihologi-dlya-vzroslyh" -> "Психолог";
            case "detskaya-psihologiya" -> "Детский психолог";
            case "neyropsihologiya" -> "Нейропсихолог";
            case "logoped" -> "Логопед";
            case "defektologia" -> "Дефектолог";
            case "preschool" -> "Дошкольный педагог";
            case "pedagogika" -> "Педагог";
            case "jurist" -> "Юрист";
            case "sport" -> "Тренер";
            case "sidelka" -> "Сиделка";
            default -> {
                String s = section.replace('-', ' ').replace('_', ' ');
                yield s.isEmpty() ? "Специалист" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
            }
        };
    }

    private static String section(String body, String start, String end) {
        int a = body.indexOf(start);
        if (a < 0) return "";
        a += start.length();
        int b = end == null ? -1 : body.indexOf(end, a);
        return (b < 0 ? body.substring(a) : body.substring(a, b)).trim();
    }

    private static String sectionUntilAny(String body, String start, String... ends) {
        int a = body.indexOf(start);
        if (a < 0) return "";
        a += start.length();
        int b = -1;
        for (String e : ends) {
            int i = body.indexOf(e, a);
            if (i >= 0 && (b < 0 || i < b)) b = i;
        }
        return (b < 0 ? body.substring(a) : body.substring(a, b)).trim();
    }

    private static String serviceNames(String raw, int max) {
        List<String> names = new ArrayList<>();
        for (String l : raw.split("\\R")) {
            String s = l.trim();
            if (s.isEmpty() || s.contains("₽") || s.toLowerCase().startsWith("длительность")) continue;
            if (!names.contains(s)) names.add(s);
            if (names.size() >= max) break;
        }
        return String.join(", ", names);
    }

    private static String sectionOf(String root) {
        String path = root.replaceAll("[?#].*$", "").replaceAll("/+$", "");
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }

    private boolean waitForListing(Page page, double timeoutMs) {
        try {
            page.waitForSelector("[data-testid='profile_action_button'], [data-testid='listing_show_more_button']",
                    new Page.WaitForSelectorOptions().setTimeout(timeoutMs));
            page.waitForTimeout(800);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void dismissCookieBanner(Page page) {
        try {
            Locator ok = page.locator("button:has-text('Хорошо')");
            if (ok.count() > 0 && ok.first().isVisible()) ok.first().click();
        } catch (Exception ignored) { }
    }

    private int countCards(Page page) {
        try {
            Object n = page.evaluate("() => document.querySelectorAll(\"[data-testid='profile_action_button']\").length");
            int c = (n instanceof Number num) ? num.intValue() : 0;
            return c > 0 ? c : extractIds(page.content()).size();
        } catch (Exception e) {
            return 0;
        }
    }

    private void autoScroll(Page page) {
        try {
            long prevHeight = -1;
            for (int i = 0; i < 30; i++) {
                page.evaluate("() => window.scrollBy(0, window.innerHeight * 0.9)");
                page.waitForTimeout(250);
                Object h = page.evaluate("() => document.body.scrollHeight");
                long cur = (h instanceof Number n) ? n.longValue() : 0;
                if (cur == prevHeight) break;
                prevHeight = cur;
            }
            page.evaluate("() => window.scrollTo(0, document.body.scrollHeight)");
            page.waitForTimeout(400);
        } catch (Exception ignored) { }
    }

    private boolean clickShowMore(Page page) {
        try {
            int before = countCards(page);
            boolean clicked = false;

            Locator btn = page.locator("button[data-testid='listing_show_more_button'], button[data-shmid='pagination_next']");
            if (btn.count() > 0) {
                try {
                    btn.first().scrollIntoViewIfNeeded();
                    page.waitForTimeout(300);
                    btn.first().click();
                    clicked = true;
                } catch (Exception e) {
                    log.debug("Профи.ру: обычный клик по «Показать ещё» не удался: {}", e.getMessage());
                }
            }
            if (!clicked) {
                Object r = page.evaluate("() => { const b = Array.from(document.querySelectorAll('button'))"
                        + ".find(x => /Показать\\s*[её]щ[её]/.test(x.textContent)); "
                        + "if (!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true; }");
                clicked = Boolean.TRUE.equals(r);
            }
            if (!clicked) return false;

            for (int i = 0; i < 24; i++) {
                page.waitForTimeout(500);
                if (countCards(page) > before) {
                    log.info("Профи.ру: «Показать ещё» — карточек было {}, стало {}", before, countCards(page));
                    return true;
                }
            }
            log.info("Профи.ру: после «Показать ещё» новых карточек не появилось (карточек {})", before);
            return false;
        } catch (Exception e) {
            log.warn("Профи.ру: «Показать ещё» не удалось: {}", e.getMessage());
            return false;
        }
    }

    private static String stripQuery(String url) {
        return url.replaceAll("[?#].*$", "");
    }

    private static String stableId(String url) {
        try {
            URI u = URI.create(url);
            return (u.getPath() == null ? url : u.getPath()).replaceAll("/+$", "");
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    private void pause() {
        try { Thread.sleep(props.getRequestDelayMs()); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String cut(String s, int n) {
        return s == null ? null : (s.length() <= n ? s : s.substring(0, n));
    }
}