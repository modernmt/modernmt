package eu.modernmt.processing.detokenizer.jflex.annotators;
import eu.modernmt.processing.detokenizer.jflex.JFlexAnnotator;

%%

%public
%class ItalianAnnotator
%extends JFlexAnnotator
%unicode
%integer
%function next
%pack
%char
%{
	protected int getStartRead() { return zzStartRead; }
	protected int getMarkedPosition() { return zzMarkedPos; }
	protected int yychar() { return yychar; }
%}

%{
    protected int quoteCount;
    protected int aposCount;

    protected void onReset() {
        this.quoteCount = 0;
        this.aposCount = 0;
    }
%}

CJK        = [\u1100-\u11FF\u2E80-\uA4CF\uA840-\uA87F\uAC00-\uD7AF\uF900-\uFAFF\uFE30-\uFE4F\uFF65-\uFFDC]
_          = " "
CURRENCY   = [\$€¢¥£]
APOS       = "&apos;"
QUOT       = "&quot;"

%{
    private int englishPossessiveCase() {
        if (this.aposCount % 2 == 1)
            this.aposCount++;

        return REMOVE_FIRST;
    }
%}

%%

/* Default Ignore */
.                                                                   { /* ignore */ }

{_}[\(\[\{\¿\¡]+{_}                                                 { return REMOVE_LAST; }
{_}[\,\.\?\!\:\;\\\%\}\]\)]+{_}                                     { return REMOVE_FIRST; }

{_}{QUOT}{_}                                                        { return this.quoteCount++ % 2 == 0 ? REMOVE_LAST : REMOVE_FIRST; }
{_}{APOS}{_}                                                        { return this.aposCount++ % 2 == 0 ? REMOVE_LAST : REMOVE_FIRST; }

{CJK}{_}{CJK}                                                       { return REMOVE_FIRST; }

/* Language Specific - English */

[[:letter:][:digit:]]{_}{APOS}[:letter:].                           { return REMOVE_FIRST; }
s{_}{APOS}{_}                                                       { return englishPossessiveCase(); }
{_}{CURRENCY}{_}                                                    { return REMOVE_LAST; }