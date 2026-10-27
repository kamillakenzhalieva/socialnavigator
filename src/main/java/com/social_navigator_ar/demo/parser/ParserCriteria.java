package com.social_navigator_ar.demo.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ParserCriteria {

    private ParserCriteria() { }

    public record Group(String code, String title, boolean requiresTargetMention, List<String> specialties) { }

    public record RuleResult(String category, int score, int strongHits, boolean autoPass, String reason) {
        public RuleResult(String category, int score, int strongHits, boolean autoPass) {
            this(category, score, strongHits, autoPass, null);
        }
    }

    public static final List<Group> GROUPS = List.of(

            new Group("psychological", "Психологические услуги", false, List.of(
                    "психолог", "клинический психолог", "детский психолог", "нейропсихолог",
                    "психотерапевт", "семейный психолог", "перинатальный психолог", "кризисный психолог",
                    "арт-терапевт", "арт-терапи", "песочная терапи", "сказкотерапи", "телесно-ориентирован",
                    "гештальт", "когнитивно-поведенческ", "супервизор", "психоаналитик", "психокоррекц")),

            new Group("educational", "Образовательные услуги", true, List.of(
                    "логопед", "учитель-логопед", "логопед-дефектолог", "афазиолог", "дефектолог",
                    "олигофренопедагог", "тифлопедагог", "сурдопедагог", "специальный педагог",
                    "коррекционный педагог", "педагог-дефектолог", "лекотек", "тьютор",
                    "аба-терапевт", "аба терапи", "aba-терапевт", "aba терапи", "аба-специалист",
                    "нейропедагог", "нейрокоррекц", "нейрогимнастик", "кинезиолог", "сенсорная интеграц",
                    "альтернативная коммуникац", "pecs", "логоритмик", "ресурсный класс", "ассистент учителя",
                    "инклюзивный педагог", "педагог доп", "учитель начальных классов", "учитель-предметник",
                    "воспитатель коррекцион", "музыкальный руководитель", "специалист по раннему развитию",
                    "раннее развитие", "подготовка к школе", "развитие речи", "постановка звуков",
                    "сурдопереводчик", "переводчик жестов", "тифлопереводчик", "брайль",
                    "специалист по жизненным навыкам", "социальные навыки", "профориентолог", "репетитор")),

            new Group("medical", "Медицинские услуги", true, List.of(
                    "детский невролог", "невролог", "нейрофизиолог", "психиатр", "детский психиатр", "эпилептолог",
                    "реабилитолог", "нейрореабилитолог", "врач лфк", "инструктор лфк", "лфк", "физический терапевт",
                    "кинезитерапевт", "эрготерапевт", "бобат", "войта", "массажист", "детский массаж",
                    "мануальный терапевт", "остеопат", "ортопед", "педиатр", "генетик", "сурдолог", "тифлолог",
                    "офтальмолог", "окулист", "отоларинголог", "стоматолог", "нутрициолог", "диетолог",
                    "медицинская сестра", "медсестра", "ботулинотерапи", "сенсорная комната")),

            new Group("legal", "Юридические услуги", true, List.of(
                    "юрист", "адвокат", "юрисконсульт", "правовая помощь", "правовая консультац",
                    "оформление инвалидности", "мсэ", "ипра", "средства реабилитации", "тср",
                    "социальные выплаты", "соцвыплат", "пособи", "опека", "попечительств", "недееспособност",
                    "защита прав", "семейный юрист", "представитель в суде", "нотариус", "медиатор")),

            new Group("social", "Социальные услуги", true, List.of(
                    "социальный работник", "соцработник", "специалист по социальной работе", "социальный педагог",
                    "сопровождающий", "личный ассистент", "ассистент", "помощник для", "сиделк", "няня",
                    "кейс-менеджер", "куратор", "координатор", "волонтер", "волонтёр",
                    "сопровождаемое проживание", "сопровождаемое трудоустройство", "организатор досуга",
                    "специалист по трудоустройству", "консультант по социальн")),

            new Group("commercial", "Услуги на коммерческой основе", true, List.of(
                    "иппотерапи", "канистерапи", "музыкотерапи", "танцевально-двигательн", "адаптивная физкультура",
                    "адаптивный спорт", "адаптивного спорта", "тренер по адаптивн", "инструктор по плаванию",
                    "развивающая студия", "центр развития", "нейротренажер", "арт-студия", "театральная студия",
                    "хореограф", "тренер", "занятия для особых", "творческая студия", "мастер-класс"))
    );

    private static final String ABBR = "(?<![\\p{L}\\d])(овз|рас|дцп|зпр|зрр|онр|тнр|сдвг|рда)(?![\\p{L}\\d])";

    private static final List<Pattern> STRONG_MARKERS = compile(
            ABBR,
            "ограниченн\\p{L}*\\s+возможност\\p{L}*",
            "ограничени\\p{L}*\\s+здоровья",
            "инклюзи\\p{L}*",
            "инвалид\\p{L}*",
            "особ\\p{L}*\\s+(дет\\p{L}*|ребен\\p{L}*|люд\\p{L}*|человек\\p{L}*|потребност\\p{L}*)",
            "особенност\\p{L}*\\s+развити\\p{L}*",
            "нарушени\\p{L}*\\s+развити\\p{L}*",
            "специальн\\p{L}*\\s+образовательн\\p{L}*\\s+потребност\\p{L}*",
            "аутизм\\p{L}*", "аутист\\p{L}*",
            "церебральн\\p{L}*\\s+паралич\\p{L}*",
            "синдром\\p{L}*\\s+даун\\p{L}*",
            "задержк\\p{L}*\\s+(психическ\\p{L}*|речев\\p{L}*|психо\\p{L}*)",
            "умственн\\p{L}*\\s+отстал\\p{L}*",
            "интеллектуальн\\p{L}*\\s+наруш\\p{L}*",
            "ментальн\\p{L}*\\s+(особенност\\p{L}*|наруш\\p{L}*|инвалид\\p{L}*)",
            "наруш\\p{L}*\\s+(слух\\p{L}*|зрени\\p{L}*|речи|опорно\\p{L}*)",
            "слабослышащ\\p{L}*", "слабовидящ\\p{L}*", "неслышащ\\p{L}*", "незрячих", "слепых", "глухих",
            "тугоухост\\p{L}*", "олигофрен\\p{L}*", "эпилепс\\p{L}*",
            "дислекси\\p{L}*", "дисграфи\\p{L}*", "дискалькули\\p{L}*", "алали\\p{L}*", "дизартри\\p{L}*",
            "афази\\p{L}*", "заикани\\p{L}*", "нейроотличн\\p{L}*",
            "лечебн\\p{L}*\\s+педагогик\\p{L}*",
            "ресурсн\\p{L}*\\s+класс\\p{L}*",
            "адаптированн\\p{L}*\\s+(образовательн\\p{L}*\\s+)?программ\\p{L}*",
            "сопровождаем\\p{L}*\\s+(проживани\\p{L}*|трудоустройств\\p{L}*)",
            "реабилитационн\\p{L}*\\s+центр\\p{L}*", "центр\\p{L}*\\s+реабилитации",
            "коррекционн\\p{L}*\\s+(школ\\p{L}*|класс\\p{L}*|учреждени\\p{L}*|центр\\p{L}*)",
            "школ\\p{L}*-интернат\\p{L}*",
            "нарушени\\p{L}*\\s+интеллект\\p{L}*",
            "аутистическ\\p{L}*");

    public enum Policy { DEFAULT, ALL, REQUIRE_OVZ, SOCIAL_LEGAL }

    public record SectionRule(Policy policy, String category, String roleKey) { }

    private static final Map<String, Pattern> ROLE_PATTERNS = Map.of(
            "psych", Pattern.compile("(?<![а-я])(?:нейро)?психолог(?:а|у|ом|е|и|ов|ам|ами|ах)?(?![а-я])"
                    + "|психотерапевт[а-я]*|арт-терапевт[а-я]*|психоаналитик[а-я]*"),
            "logoped", Pattern.compile("(?<![а-я])логопед(?:а|у|ом|е|ы|ов|ам|ами|ах)?(?![а-я])|афазиолог[а-я]*"),
            "defect", Pattern.compile("дефектолог[а-я]*|олигофренопедагог[а-я]*|тифлопедагог[а-я]*|сурдопедагог[а-я]*"
                    + "|(?<![а-я])логопед(?:а|у|ом|е|ы|ов|ам|ами|ах)?(?![а-я])"));

    public static final Map<String, SectionRule> SECTION_RULES = Map.ofEntries(
            Map.entry("logoped",               new SectionRule(Policy.ALL, "educational", "logoped")),
            Map.entry("defektologia",          new SectionRule(Policy.ALL, "educational", "defect")),
            Map.entry("detskaya-psihologiya",  new SectionRule(Policy.ALL, "psychological", "psych")),
            Map.entry("neyropsihologiya",      new SectionRule(Policy.ALL, "psychological", "psych")),
            Map.entry("psihologi-dlya-vzroslyh", new SectionRule(Policy.ALL, "psychological", "psych")),
            Map.entry("preschool",             new SectionRule(Policy.REQUIRE_OVZ, "educational", null)),
            Map.entry("pedagogika",            new SectionRule(Policy.REQUIRE_OVZ, "educational", null)),
            Map.entry("sport",                 new SectionRule(Policy.REQUIRE_OVZ, "commercial", null)),
            Map.entry("sidelka",               new SectionRule(Policy.REQUIRE_OVZ, "social", null)),
            Map.entry("jurist",                new SectionRule(Policy.SOCIAL_LEGAL, "legal", null))
    );

    public static SectionRule sectionRule(String section) {
        return section == null ? null : SECTION_RULES.get(section);
    }

    private static final List<Pattern> SOCIAL_LEGAL_EXTRA = compile(
            "соцвыплат\\p{L}*",
            "социальн\\p{L}*\\s+(выплат\\p{L}*|пособи\\p{L}*|защит\\p{L}*|обеспечени\\p{L}*|помощ\\p{L}*|поддержк\\p{L}*|юрист\\p{L}*)",
            "пенси\\p{L}*", "(?<![\\p{L}\\d])мсэ(?![\\p{L}\\d])", "(?<![\\p{L}\\d])ипра(?![\\p{L}\\d])",
            "опек\\p{L}*", "попечител\\p{L}*", "недееспособ\\p{L}*", "льгот\\p{L}*", "субсиди\\p{L}*",
            "детей-сирот\\p{L}*", "многодетн\\p{L}*", "малоимущ\\p{L}*", "пособи\\p{L}*",
            "средств\\p{L}*\\s+реабилитаци\\p{L}*", "технически\\p{L}*\\s+средств\\p{L}*");

    private static final List<String> WEAK_MARKERS = List.of(
            "коррекци", "реабилитаци", "абилитаци", "особенн", "нейрокоррекц", "сенсорн");

    public static final List<String> STOP_WORDS = List.of(
            "егэ", "огэ", "репетитор по математике", "репетитор по английскому",
            "репетитор по русскому", "репетитор по литературе", "маникюр", "косметолог", "массаж лица",
            "астролог", "таро", "гадани", "нумеролог", "эзотерик", "коуч по бизнесу",
            "бизнес-тренер", "менеджер по продажам", "маркетолог", "ремонт", "грузоперевоз",
            "эксперт по привлечению", "школа блогеров");

    public static final List<String> TARGET_KEYWORDS = List.of(
            "овз", "инклюзи", "инвалидност", "аутизм", "дцп", "синдром дауна");

    public static final List<String> ASTRAKHAN_REGION = List.of(
            "астрахан", "ахтубинск", "харабал", "знаменск", "камызяк", "икряное", "икрянин",
            "нариманов", "володарск", "красный яр", "енотаевк", "лиман",
            "началово", "черный яр", "чёрный яр", "мумра", "аксарайск");

    public static final List<String> REMOTE_MARKERS = List.of(
            "дистанцион", "онлайн", "online", "удаленно", "удалённо");

    public static final List<String> HH_QUERIES = List.of(
            "психолог", "педагог-психолог", "нейропсихолог",
            "логопед ОВЗ", "учитель-логопед", "дефектолог", "учитель-дефектолог", "тьютор ОВЗ",
            "специалист по инклюзии", "педагог инклюзивное образование", "коррекционный педагог",
            "сурдопедагог", "тифлопедагог", "педагог РАС", "АВА терапевт", "ассистент ОВЗ",
            "невролог детский", "врач ЛФК дети", "эрготерапевт", "реабилитолог",
            "юрист инвалиды", "специалист по социальной работе", "социальный работник инвалиды",
            "сопровождающий инвалидов", "няня особый ребенок", "адаптивная физкультура");

    private static List<Pattern> compile(String... regexes) {
        List<Pattern> list = new ArrayList<>();
        for (String r : regexes) list.add(Pattern.compile(r));
        return List.copyOf(list);
    }

    public static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    public static boolean isAstrakhanRegion(String text) {
        String t = norm(text);
        return ASTRAKHAN_REGION.stream().anyMatch(k -> t.contains(norm(k)));
    }

    public static boolean looksRemoteOnly(String text) {
        String t = norm(text);
        return REMOTE_MARKERS.stream().anyMatch(t::contains) && !isAstrakhanRegion(t);
    }

    public static boolean hasStopWord(String text) {
        String t = norm(text);
        return STOP_WORDS.stream().anyMatch(w -> t.contains(norm(w)));
    }

    public static int countStrongMarkers(String text) {
        String t = norm(text);
        int n = 0;
        for (Pattern p : STRONG_MARKERS) if (p.matcher(t).find()) n++;
        return n;
    }

    private static int countSocialLegal(String text) {
        String t = norm(text);
        int n = 0;
        for (Pattern p : SOCIAL_LEGAL_EXTRA) if (p.matcher(t).find()) n++;
        return n;
    }

    private static int countWeakMarkers(String text) {
        String t = norm(text);
        int n = 0;
        for (String w : WEAK_MARKERS) if (t.contains(w)) n++;
        return n;
    }

    private static Group detectGroup(String text) {
        String t = norm(text);
        if (t.isBlank()) return null;
        for (Group g : GROUPS) {
            for (String s : g.specialties()) if (t.contains(norm(s))) return g;
        }
        return null;
    }

    public static String detectCategory(String text) {
        Group g = detectGroup(text);
        return g == null ? null : g.code();
    }

    public static RuleResult evaluate(String headline, String fullText, String section) {
        String hl = headline == null ? "" : headline;
        String ft = fullText == null ? "" : fullText;
        String all = hl + " " + ft;
        if (hasStopWord(all)) return new RuleResult(null, 0, 0, false, "стоп-слово");

        SectionRule sr = sectionRule(section);
        Policy policy = sr == null ? Policy.DEFAULT : sr.policy();

        String evidence = norm(hl + " " + servicesPart(ft));
        Pattern role = sr == null ? ROLE_PATTERNS.get("psych")
                : (sr.roleKey() == null ? null : ROLE_PATTERNS.get(sr.roleKey()));
        boolean roleOk = role != null && role.matcher(evidence).find();

        int strong = countStrongMarkers(all);
        int weak = countWeakMarkers(all);

        if (roleOk) {
            String cat = sr == null ? "psychological" : sr.category();
            return new RuleResult(cat, Math.min(100, 70 + Math.min(30, strong * 10 + weak * 3)), strong, true, null);
        }

        boolean legal = policy == Policy.SOCIAL_LEGAL;
        int hits = legal ? strong + countSocialLegal(all) : strong;
        return requireMarkers(nonPsychCategory(hl, ft, sr), hits, strong, weak, legal);
    }

    private static String nonPsychCategory(String headline, String fullText, SectionRule sr) {
        for (String text : new String[]{headline, fullText}) {
            String t = norm(text);
            if (t.isBlank()) continue;
            for (Group g : GROUPS) {
                if ("psychological".equals(g.code())) continue;
                for (String sp : g.specialties()) if (t.contains(norm(sp))) return g.code();
            }
        }
        if (sr == null) return null;
        return "psychological".equals(sr.category()) ? "educational" : sr.category();
    }

    private static String servicesPart(String text) {
        int i = text.indexOf("Услуги:");
        return i < 0 ? "" : text.substring(i + 7);
    }

    private static RuleResult requireMarkers(String category, int hits, int strong, int weak, boolean legal) {
        if (hits == 0) {
            String why = legal ? "нет маркеров социального юриста (инвалидность, МСЭ, пособия…)"
                               : "нет упоминания ОВЗ / инклюзии / РАС / ДЦП / реабилитационных центров";
            return weak >= 2 ? new RuleResult(category, 40, 0, false, "серая зона: решает ИИ")
                             : new RuleResult(category, 0, 0, false, why);
        }
        int score = category == null ? 45 + Math.min(30, (hits - 1) * 10)
                                     : 60 + Math.min(40, (hits - 1) * 10 + weak * 3);
        return new RuleResult(category, Math.min(100, score), strong, false, null);
    }

    public static RuleResult evaluate(String headline, String fullText) {
        return evaluate(headline, fullText, null);
    }

    public static int ruleScore(String text) {
        return evaluate("", text).score();
    }

    public static String promptDescription() {
        StringBuilder sb = new StringBuilder();
        for (Group g : GROUPS) {
            sb.append("- ").append(g.code()).append(" (").append(g.title()).append("): ")
              .append(String.join(", ", g.specialties())).append('\n');
        }
        return sb.toString();
    }
}
