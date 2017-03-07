//package eu.modernmt.processing.xmessage;
//
//import eu.modernmt.model.MultiOptionsToken;
//import eu.modernmt.model.Translation;
//import eu.modernmt.model.Word;
//
///**
// * Created by davide on 08/04/16.
// */
//public class XFormatWord extends Word implements MultiOptionsToken {
//
//    private static final Transformation TRANSFORMATION = (Transformation) (source, target) -> {
//        if (source != null && source instanceof XFormatWord && target instanceof XFormatWord) {
//            XFormat format = ((XFormatWord) source).getFormat();
//            ((XFormatWord) target).setFormat(format);
//        }
//    };
//
//    private XFormat format;
//    private boolean choicesTranslated = false;
//
//    public XFormatWord(XFormat format) {
//        super(format.getPlaceholder(), format.getPlaceholder());
//        this.format = format;
//        this.setTransformation(TRANSFORMATION);
//    }
//
//    public XFormatWord(XFormat format, String rightSpace) {
//        super(format.getPlaceholder(), format.getPlaceholder(), rightSpace);
//        this.format = format;
//        this.setTransformation(TRANSFORMATION);
//    }
//
//    public XFormatWord(XFormat format, String rightSpace, boolean rightSpaceRequired) {
//        super(format.getPlaceholder(), format.getPlaceholder(), rightSpace, rightSpaceRequired);
//        this.format = format;
//        this.setTransformation(TRANSFORMATION);
//    }
//
//    public XFormatWord(String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
//        super(text, placeholder, rightSpace, rightSpaceRequired);
//        this.setTransformation(TRANSFORMATION);
//    }
//
//    public XFormat getFormat() {
//        return format;
//    }
//
//    public void setFormat(XFormat format) {
//        this.format = format;
//    }
//
//    @Override
//    public String getText() {
//        return format == null ? super.getText() : format.toString();
//    }
//
//    @Override
//    public boolean hasText() {
//        return format != null || super.hasText();
//    }
//
//    @Override
//    public String getPlaceholder() {
//        return format == null ? super.getPlaceholder() : format.getPlaceholder();
//    }
//
//    @Override
//    public String toString() {
//        return format == null ? super.toString() : format.toString();
//    }
//
//    // MultiOptionsToken
//
//    @Override
//    public String[] getSourceOptions() {
//        XChoiceFormat choice = (XChoiceFormat) format;
//        XChoiceFormat.Choice[] choices = choice.choices;
//        String[] options = new String[choices.length];
//
//        for (int i = 0; i < options.length; i++)
//            options[i] = choices[i].value;
//
//        return options;
//    }
//
//    @Override
//    public void setTranslatedOptions(Translation[] translations) {
//        XChoiceFormat choiceFormat = (XChoiceFormat) format;
//        XChoiceFormat.Choice[] choices = choiceFormat.choices;
//
//        XChoiceFormat.Choice[] translatedChoices = new XChoiceFormat.Choice[choices.length];
//        for (int i = 0; i < translatedChoices.length; i++) {
//            Translation translation = translations[i];
//            XChoiceFormat.Choice choice = choices[i];
//
//            translatedChoices[i] = new XChoiceFormat.Choice(choice.key, choice.divider, translation.toString());
//        }
//
//        format = new XChoiceFormat(choiceFormat.id, choiceFormat.index, choiceFormat.type, translatedChoices);
//        choicesTranslated = true;
//    }
//
//    @Override
//    public boolean hasTranslatedOptions() {
//        return format == null || !(format instanceof XChoiceFormat) || choicesTranslated;
//    }
//}
