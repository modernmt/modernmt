package eu.modernmt.processing.transliteration;

public class Serbian {

    public static String toCyrillic(String string) {
        int length = string.length();

        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c1 = string.charAt(i);
            char c2 = i + 1 < length ? string.charAt(i + 1) : '\0';

            i += appendCyrillic(c1, c2, builder);
        }

        return builder.toString();
    }

    private static int appendCyrillic(char c1, char c2, StringBuilder out) {
        switch (c1) {
            case 'A':
                out.append('А');
                return 0;
            case 'a':
                out.append('а');
                return 0;
            case 'B':
                out.append('Б');
                return 0;
            case 'b':
                out.append('б');
                return 0;
            case 'V':
                out.append('В');
                return 0;
            case 'v':
                out.append('в');
                return 0;
            case 'G':
                out.append('Г');
                return 0;
            case 'g':
                out.append('г');
                return 0;
            case 'D':
                if (c2 == 'ž') {
                    out.append('Џ');
                    return 1;
                } else {
                    out.append('Д');
                    return 0;
                }
            case 'd':
                if (c2 == 'ž') {
                    out.append('џ');
                    return 1;
                } else {
                    out.append('д');
                    return 0;
                }
            case 'Đ':
                out.append('Ђ');
                return 0;
            case 'đ':
                out.append('ђ');
                return 0;
            case 'E':
                out.append('Е');
                return 0;
            case 'e':
                out.append('е');
                return 0;
            case 'Ž':
                out.append('Ж');
                return 0;
            case 'ž':
                out.append('ж');
                return 0;
            case 'Z':
                out.append('З');
                return 0;
            case 'z':
                out.append('з');
                return 0;
            case 'I':
                out.append('И');
                return 0;
            case 'i':
                out.append('и');
                return 0;
            case 'J':
                out.append('Ј');
                return 0;
            case 'j':
                out.append('ј');
                return 0;
            case 'K':
                out.append('К');
                return 0;
            case 'k':
                out.append('к');
                return 0;
            case 'L':
                if (c2 == 'j') {
                    out.append('Љ');
                    return 1;
                } else {
                    out.append('Л');
                    return 0;
                }
            case 'l':
                if (c2 == 'j') {
                    out.append('љ');
                    return 1;
                } else {
                    out.append('л');
                    return 0;
                }
            case 'M':
                out.append('М');
                return 0;
            case 'm':
                out.append('м');
                return 0;
            case 'N':
                if (c2 == 'j') {
                    out.append('Њ');
                    return 1;
                } else {
                    out.append('Н');
                    return 0;
                }
            case 'n':
                if (c2 == 'j') {
                    out.append('њ');
                    return 1;
                } else {
                    out.append('н');
                    return 0;
                }
            case 'O':
                out.append('О');
                return 0;
            case 'o':
                out.append('о');
                return 0;
            case 'P':
                out.append('П');
                return 0;
            case 'p':
                out.append('п');
                return 0;
            case 'R':
                out.append('Р');
                return 0;
            case 'r':
                out.append('р');
                return 0;
            case 'S':
                out.append('С');
                return 0;
            case 's':
                out.append('с');
                return 0;
            case 'T':
                out.append('Т');
                return 0;
            case 't':
                out.append('т');
                return 0;
            case 'Ć':
                out.append('Ћ');
                return 0;
            case 'ć':
                out.append('ћ');
                return 0;
            case 'U':
                out.append('У');
                return 0;
            case 'u':
                out.append('у');
                return 0;
            case 'F':
                out.append('Ф');
                return 0;
            case 'f':
                out.append('ф');
                return 0;
            case 'H':
                out.append('Х');
                return 0;
            case 'h':
                out.append('х');
                return 0;
            case 'C':
                out.append('Ц');
                return 0;
            case 'c':
                out.append('ц');
                return 0;
            case 'Č':
                out.append('Ч');
                return 0;
            case 'č':
                out.append('ч');
                return 0;
            case 'Š':
                out.append('Ш');
                return 0;
            case 'š':
                out.append('ш');
                return 0;
            default:
                out.append(c1);
                return 0;
        }
    }

    public static String toLatin(String string) {
        int length = string.length();

        StringBuilder builder = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            char c = string.charAt(i);
            appendLatin(c, builder);
        }

        return builder.toString();
    }

    private static void appendLatin(char c, StringBuilder out) {
        switch (c) {
            case 'А':
                out.append('A');
                break;
            case 'а':
                out.append('a');
                break;
            case 'Б':
                out.append('B');
                break;
            case 'б':
                out.append('b');
                break;
            case 'В':
                out.append('V');
                break;
            case 'в':
                out.append('v');
                break;
            case 'Г':
                out.append('G');
                break;
            case 'г':
                out.append('g');
                break;
            case 'Д':
                out.append('D');
                break;
            case 'д':
                out.append('d');
                break;
            case 'Ђ':
                out.append('Đ');
                break;
            case 'ђ':
                out.append('đ');
                break;
            case 'Е':
                out.append('E');
                break;
            case 'е':
                out.append('e');
                break;
            case 'Ж':
                out.append('Ž');
                break;
            case 'ж':
                out.append('ž');
                break;
            case 'З':
                out.append('Z');
                break;
            case 'з':
                out.append('z');
                break;
            case 'И':
                out.append('I');
                break;
            case 'и':
                out.append('i');
                break;
            case 'Ј':
                out.append('J');
                break;
            case 'ј':
                out.append('j');
                break;
            case 'К':
                out.append('K');
                break;
            case 'к':
                out.append('k');
                break;
            case 'Л':
                out.append('L');
                break;
            case 'л':
                out.append('l');
                break;
            case 'Љ':
                out.append("Lj");
                break;
            case 'љ':
                out.append("lj");
                break;
            case 'М':
                out.append('M');
                break;
            case 'м':
                out.append('m');
                break;
            case 'Н':
                out.append('N');
                break;
            case 'н':
                out.append('n');
                break;
            case 'Њ':
                out.append("Nj");
                break;
            case 'њ':
                out.append("nj");
                break;
            case 'О':
                out.append('O');
                break;
            case 'о':
                out.append('o');
                break;
            case 'П':
                out.append('P');
                break;
            case 'п':
                out.append('p');
                break;
            case 'Р':
                out.append('R');
                break;
            case 'р':
                out.append('r');
                break;
            case 'С':
                out.append('S');
                break;
            case 'с':
                out.append('s');
                break;
            case 'Т':
                out.append('T');
                break;
            case 'т':
                out.append('t');
                break;
            case 'Ћ':
                out.append('Ć');
                break;
            case 'ћ':
                out.append('ć');
                break;
            case 'У':
                out.append('U');
                break;
            case 'у':
                out.append('u');
                break;
            case 'Ф':
                out.append('F');
                break;
            case 'ф':
                out.append('f');
                break;
            case 'Х':
                out.append('H');
                break;
            case 'х':
                out.append('h');
                break;
            case 'Ц':
                out.append('C');
                break;
            case 'ц':
                out.append('c');
                break;
            case 'Ч':
                out.append('Č');
                break;
            case 'ч':
                out.append('č');
                break;
            case 'Џ':
                out.append("Dž");
                break;
            case 'џ':
                out.append("dž");
                break;
            case 'Ш':
                out.append('Š');
                break;
            case 'ш':
                out.append('š');
                break;
            default:
                out.append(c);
                break;
        }
    }

}
