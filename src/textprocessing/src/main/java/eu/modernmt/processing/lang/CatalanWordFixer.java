package eu.modernmt.processing.lang;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.util.*;

public class CatalanWordFixer extends TextProcessor<Translation, Translation> {

    public CatalanWordFixer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        if (!Language.CATALAN.getLanguage().equals(targetLanguage.getLanguage()))
            throw new UnsupportedLanguageException(targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        for (Word word : translation.getWords()) {
            String placeholder = word.getPlaceholder().toLowerCase();

            if (DICTIONARY.contains(placeholder)) {
                String text = word.toString(false);
                text = text.replace("ll", "l·l");
                text = text.replace("LL", "L·L");

                word.setText(text);
            }
        }

        return translation;
    }

    private static final Set<String> DICTIONARY = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("illumin", "allucinaràs", "illès", "recristallitzat", "sollicitis", "illusi", "berille",
                    "illegalitzar", "recollectores", "prehellèniques", "berilli", "milleni", "illuss", "sollicitin",
                    "illegalment", "gorilles", "collecci", "àllida", "excellent", "hellènic", "illusionats",
                    "allucinaves", "collocats", "propellant", "intellecte", "instrallar", "tallós", "íllid", "cellular",
                    "villes", "titillar", "íllic", "àllids", "collectives", "parallax", "àllica", "milligram",
                    "sillàbic", "microcristallina", "sollicitaven", "constellaci", "fallàcia", "protocollitzada",
                    "àllics", "fallàcies", "controllats", "recollocarem", "millisegon", "collecciona", "collocaven",
                    "collisions", "columella", "intelli", "sollicitant", "tranquillitzaria", "sigillada", "collegis",
                    "espinelles", "oscillava", "illuminant", "ílliques", "alliteraci", "collocaràs", "protocollitzaci",
                    "galleries", "intracellular", "collateral", "parallelismes", "vacillant", "solliciteu",
                    "collisiona", "repellent", "sibilla", "solliciten", "sollicitem", "millers", "illocalitzable",
                    "sollicites", "cancelladarefresh", "collecitus", "maxillofacial", "alloglots", "gallesos",
                    "recollector", "intelligència", "illusoris", "scancellar", "metallúrgica", "tallaxi", "cellules",
                    "ellisis", "capillars", "allegada", "vacillants", "pupillatge", "illuminats", "impolluts", "fallus",
                    "collegues", "interpellant", "apellat", "cancellàrem", "allegaci", "alleluies", "apellar",
                    "allegar", "allegat", "tranquillitza", "collocaran", "illògica", "allucin", "metallitzat",
                    "alludida", "umbella", "illògics", "collocant", "tranquillitzi", "corallins", "installables",
                    "colleccionistes", "illustrator", "illegitimitat", "oscillar", "èlluladelcos", "collega",
                    "oscillat", "sollicitar", "rebellions", "collegi", "collisionador", "sollicitat", "oscilladors",
                    "pellicula", "pollinitzen", "allèrgies", "calligràfic", "corallí", "controllant", "recollectar",
                    "coralligen", "apellen", "intallaci", "illuminessin", "monocellular", "allumini",
                    "tranquillitzadora", "anullades", "millenària", "ellipsoide", "íllab", "anullats", "parallelitzaci",
                    "oscillants", "collapsant", "illegals", "instillar", "allèrgica", "siderometallúrgica", "èllits",
                    "rallis", "intelligentment", "constellacions", "apelles", "anticollisi", "oscilloscopi",
                    "destillen", "millilítre", "desinstallar", "penicillina", "millenaristes", "desinstallat",
                    "illegítimes", "desinstallacions", "intellectual", "intellectualitat", "varicella", "xarello",
                    "colleccionista", "recopillar", "illustratiu", "abdullà", "ellíptic", "hidroxipropilmetilcellulosa",
                    "illuminareu", "colleci", "sollicitaci", "xitxarellos", "millimètrica", "ampulla", "collisionant",
                    "colleccionisme", "illuminat", "reinstalla", "sollicitada", "pollinitzar", "pollinitzat",
                    "illuminar", "facillitar", "ocolleccions", "rebellava", "vallllossera", "flagellades", "reinstalli",
                    "vacillava", "micellar", "intranquillitza", "pluricellular", "destillades", "íllea", "installin",
                    "preinstallaci", "sollicitadora", "flagellaci", "installis", "collocave", "millenarista",
                    "collocacions", "parallaxi", "recollectades", "raquelledesma", "pantelleria", "cellul",
                    "excellentment", "milligrams", "destillat", "sollicltant", "oscillacions", "flagellada",
                    "parallitzat", "collocava", "illeses", "pupilla", "alludeixi", "desinstalleu", "extracellular",
                    "collapsi", "repelleix", "rellatius", "antellaci", "pollinitzadors", "palliar", "galleses",
                    "micella", "sigillosament", "collapsar", "collapsa", "collapsat", "millenni", "collapse", "medulla",
                    "parellel", "palliaci", "papilles", "brusselles", "elludeix", "allicient", "parallitzada",
                    "collectivament", "collaterals", "miscellani", "pallidots", "apellatiu", "revelli", "preinstalleu",
                    "anullar", "anullat", "impollut", "amoxicillina", "millenis", "ciberillusionista", "nitrocellulosa",
                    "anullae", "desinstallada", "apellarem", "calligrafies", "cancellarien", "gallaci",
                    "intellectualment", "parcellari", "collisionat", "miscellania", "recollectada", "celulloses",
                    "interpellaci", "desinstallaci", "reinstalleu", "alludeix", "miscellànies", "miscellanis",
                    "collaboratiu", "collaboradores", "bimetallisme", "illustren", "bacillar", "illustres",
                    "tarallejar", "metalloide", "collectivitats", "cristallí", "follicle", "collagen", "allucinacions",
                    "millenaris", "collapsen", "collaboracio", "excellècia", "collapses", "venocapillar",
                    "intellectuals", "metalloproteases", "reinstallaci", "collegiales", "deoinstallaci", "collisi",
                    "illuminaven", "taralleja", "colloquial", "cancellaci", "oscillen", "pluricellulars", "àllides",
                    "collaci", "pelli", "mullà", "novellista", "tranquillitzaves", "hesbollah", "anullable",
                    "cellacollapse", "protocollitzo", "sollucionar", "clorofilla", "alleloquímics", "interpellar",
                    "illustracions", "collo", "installacions", "installador", "excelleixen", "cancellada", "íllabes",
                    "reinstallar", "interstellar", "illos", "reinstallat", "cellulosa", "cancell​ació", "installable",
                    "desfibrilladors", "apello", "sollici", "sollicit", "illuminada", "telluri", "nullitat",
                    "collapsin", "maxillars", "illuminaci", "damisella", "apella", "xixerello", "parcella", "tilla",
                    "intelliguèntsia", "valliri", "axilla", "polluents", "desinstallador", "callibrat",
                    "infallibilitat", "interpellen", "allergens", "scollecci", "illms", "millenarisme", "allegoria",
                    "anullador", "pellis", "tranquillitzaran", "illma", "sillabari", "capillaritat",
                    "collaborativament", "vacillacions", "intellegent", "illustradora", "èllica", "mosella",
                    "allucinats", "aquarella", "reinstallaran", "molluscos", "celles", "pullulant", "collacions",
                    "illustratius", "tallaxians", "tallits", "estellar", "recollocar", "illustri", "recollocat",
                    "illustre", "collaps", "deumillèsima", "illustrativa", "collaboraci", "èllics", "riainfallible",
                    "destilleria", "collaboraràs", "sigilloses", "collegial", "collaboraven", "sollicitarem",
                    "collegiat", "allèrgia", "cristallina", "allèrgic", "postilla", "pollinitzaci", "cancellaran",
                    "colla", "desinstallant", "illustra", "estellars", "metallíferes", "metallisteria", "intranquilla",
                    "desillusionaràs", "vacillaci", "collocarem", "titullat", "caravella", "solliciar", "ellipses",
                    "illustradors", "èllit", "hepatocellular", "oscillant", "allà", "cristallins", "encollaboraci",
                    "èllic", "èllid", "entitatcollaboradora", "unainstallaci", "pellicular", "cristallines",
                    "desfibrillaci", "sillogisme", "tranquillitant", "destilleries", "collecciono", "fellaci",
                    "colleccions", "ninstalleu", "collectiu", "collisionen", "caramellitzat", "apellant", "flagellin",
                    "tolpellícula", "rescolloques", "illuminacions", "medullar", "raquimedullar", "illegal", "aiatollà",
                    "palladi", "allerg", "novella", "illegítim", "hidrometallúrgia", "colleccio", "ellíptics",
                    "millisegons", "allucinant", "collaboraran", "cancellarem", "anulli", "sollicitaran", "cancellareu",
                    "franella", "intelligible", "parallel", "unicellular", "illegítima", "colleccionava", "l'aiatollà",
                    "illuminadors", "installades", "allèrgens", "cancellant", "recolloca", "mullah", "novelles",
                    "millibars", "compostella", "èllet", "metallúrgia", "tessella", "tranquillitat", "metallúrgic",
                    "ellíptica", "cancellacio", "collapsada", "allah", "intell¡gent", "destillats", "vacillaves",
                    "cristallitzen", "pellicules", "gallípoli", "illegls", "hellenístics", "desinstallaran", "allusius",
                    "infallibles", "molluscs", "vacillaven", "reparcellacions", "pasarelles", "allucinants",
                    "multisillàbica", "calligràfics", "cristallitzaci", "illuminador", "collaboràvem",
                    "tranquillitzador", "ralli", "pollinitzant", "expellir", "anulla", "otello", "calligràfica",
                    "pollinitzador", "millions", "calligrafia", "aiatollàs", "colleccionat", "colleccionar",
                    "pallidesa", "collègit", "illimitades", "salmonella", "desillusionar", "desillusionat", "illícites",
                    "postinstallaci", "colloides", "superintelligència", "elliminar", "colleccion", "descollocats",
                    "sollícita", "subcollecci", "tillita", "recollectora", "callígraf", "recollectors", "escilla",
                    "illesa", "allegues", "sollicitants", "íllaba", "cristallitza", "desillusi", "installada",
                    "cancellar", "cancellat", "collegiades", "brucellosi", "excellents", "millímetres", "fallera",
                    "protocollari", "hellè", "millilitres", "excollega", "installaci", "gallès", "illegalitats",
                    "tranquilles", "flagellar", "cristallinitat", "cancellats", "tranquillitzant", "cancellaràs",
                    "alludia", "illusionisme", "sollicituts", "cancellad", "passarella", "cancellava", "ellí",
                    "collector", "collaboressin", "allusiu", "mollecular", "destillava", "allegaran", "collabor",
                    "oscilla", "desillusionem", "oscillaran", "collocades", "interestellars", "palliatives", "oscille",
                    "illustrador", "caramellitzades", "intelligències", "colleccionant", "illicitud", "rebelli",
                    "anullis", "cancelles", "dulla", "hipocellular", "illustrant", "cancelleu", "installessin",
                    "paralleles", "constelladors", "rebello", "depillar", "millenàries", "allega", "anullin",
                    "belligerància", "passarelles", "rebella", "superintelligent", "sollicitd", "sollicita",
                    "malleabilitat", "hacancellat", "solliciti", "sollicito", "cancellem", "illustrades", "hellenista",
                    "cancellen", "cancellés", "cellaexpand", "escollocaenunaposici", "malleable", "palladianisme",
                    "allucinogen", "apellaci", "pupilles", "filloxera", "selleccionats", "collegiada", "èllula",
                    "millímetre", "hellenístic", "cavillant", "illimitada", "anullaven", "instillaci", "collutori",
                    "cristallografia", "aquilles", "capillar", "collegiaci", "allucinaci", "allego", "allegories",
                    "allergogens", "tranquillizant", "droguesillegals", "palliativa", "ll", "anulles", "anullen",
                    "anullem", "apollònia", "allèrgiques", "vacillis", "cristallitzar", "anulleu", "cristallitzat",
                    "preinstallar", "preinstallat", "rebellies", "telenovella", "collectivitat", "collectivitas",
                    "adullaci", "dòllars", "inintelligent", "millennis", "recollecta", "illògic", "idilli",
                    "protocollitzar", "protocollitzat", "tarallejant", "illustrats", "collectors", "allòctones",
                    "cancellis", "collaboratives", "palliatius", "vacilla", "pupillar", "millènis", "collaboracionista",
                    "collecciones", "cancellin", "cristallitzada", "pollen", "allucino", "cancella", "tessellaci",
                    "millennistes", "tiller", "oscillaci", "allucina", "allusions", "elles", "colleccionables", "illm",
                    "illustrar", "anullaran", "cancello", "celluloses", "illús", "appellants", "collaborant",
                    "illustrat", "millenari", "reinstallant", "cancelli", "gorilla", "illusionista", "protocollària",
                    "preinstallades", "bellicosa", "illustrava", "intelligent", "metallitzada", "retroilluminaci",
                    "metallúrgiques", "illus", "villa", "nulla", "collecionista", "illuminen", "collaborem", "excellir",
                    "collaboren", "parcelles", "collabores", "destillada", "collocada", "bucomaxillofacial", "illusori",
                    "cellulars", "installaciant", "collocaci", "desinstalla", "destillaci", "supercollisionador",
                    "íllies", "illumineu", "allucinin", "collecta", "selleccioni", "fallopi", "nulles", "installeu",
                    "mesullam", "sellecciona", "collaboreu", "installes", "colloca", "mollusc", "installem", "colloco",
                    "parallelograms", "desintallaran", "inintelligible", "installés", "installen", "allergènics",
                    "protocollàries", "collegials", "allotr", "installadora", "allots", "precollegiacions",
                    "sollicituds", "parallelament", "epillèpsia", "tranquillíssim", "ombrella", "anullarem",
                    "preinstallada", "palliatiu", "installadors", "bruselles", "recollocaci", "excellència",
                    "intercollegial", "desinstalli", "desinstallo", "collaboracions", "hellenisme", "tesselles",
                    "maxillar", "desinstallis", "rolló", "telenovelles", "allota", "rebellat", "collegiala", "vacillar",
                    "allegacions", "corolla", "anullant", "anguilliforme", "allegant", "colloquialment", "ellíptiques",
                    "tranquillitzis", "cellofana", "rebellar", "illuminin", "pasarella", "colloque", "collaborin",
                    "collaboris", "repellides", "cancellable", "installats", "installadores", "tranquilla",
                    "maxillofacials", "ellipse", "sollicitut", "infallible", "cella", "ellipsi", "circumcellions",
                    "metalloprote", "íllica", "ellipso", "destillacions", "interestellar", "installat", "installar",
                    "cellebra", "illegible", "colloqui", "hesbollà", "llaboració", "allegats", "tranquillitzeu",
                    "sinstalleu", "coll", "alleluia", "intallar", "rebellen", "mollècula", "pellícula", "collapsaran",
                    "exellent", "desfibrillador", "ombrelles", "illegalitat", "íllics", "controllar", "celle",
                    "collocadíssim", "marcella", "destilla", "illusionin", "collaboracionistes", "installaria",
                    "illuminarien", "illícit", "collaborava", "sollicitàvem", "èllules", "colloquials", "illuminades",
                    "tranquillitzants", "installava", "collaborados", "rebelleu", "collaborador", "tranqilles",
                    "ampullars", "caramellitzaci", "colloquis", "parallelogram", "colloquessin", "collaboradora",
                    "multicollinealitat", "fibrillaci", "collaboradors", "alliats", "oscillador", "miscellània",
                    "anullaci", "hipoallergènics", "cartella", "illuminava", "papilloma", "millèsimes", "precollegiats",
                    "mollecula", "colloquin", "empallideixen", "collaborativa", "installo", "illimitat", "sillàbica",
                    "illumini", "installacio", "appellant", "apollo", "àlliques", "drusilla", "caramellitzada",
                    "fallible", "apellacions", "installa", "apellants", "cellebrat", "anullacions", "cellebrar",
                    "polluci", "tranquillitz", "tillers", "èllencia", "collaboratius", "intellectualisme", "installi",
                    "arallel", "anullada", "propellent", "parallels", "illimitats", "cancellades", "tranquillitzar",
                    "illimitadament", "galles", "sollicitava", "installac", "pellícules", "millimetrat", "parallela",
                    "pellagra", "sillàbics", "intelligents", "tranquillitzat", "collegiats", "illumina",
                    "multicellulars", "illumine", "parallelisme", "rebellia", "particella", "collectiva", "collectius",
                    "marcellí", "sollicitud", "alluvial", "installaren", "installarem", "ellèbor", "colloqueu",
                    "colloques", "celluloide", "colloquen", "collusi", "alleg", "installatnoactual", "excelleix",
                    "sollictud", "cellula", "sollicitats", "colloquem", "millilitre", "allucinem", "ancella",
                    "collocar", "illustraci", "collocat", "recollecci", "ssollicituds", "unicellulars", "illusions",
                    "colleg", "collaborat", "illustrada", "collaborar", "collegiacions", "collabori", "installà",
                    "collec", "inocullar", "collaboro", "illícita", "tranquillament", "collabora", "illusiona",
                    "sollicitades", "installant", "parcellària", "collaborades", "illícits", "talli", "cancellacions",
                    "fotocellulars", "allucinar", "allucinat", "allusi", "novellistes", "ellaborat", "xinelles",
                    "aquarelles", "ellaborar", "inapellable", "repellir", "repellit", "installaran", "collectivisme",
                    "bellicista", "illusionat", "àllid", "tallòfit", "àllic", "àllia")));

}
