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

CJ         = [\u3100-\u312f\u3040-\u309F\u30A0-\u30FF\u31F0-\u31FF\u3300-\u337f\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff\uff65-\uff9f]
_          = " "
LETTER     = !(![:letter:]|{CJ})
CURRENCY   = [\$€¢¥]
APOS       = "&apos;"
QUOT       = "&quot;"

%{
    private int englishPossessiveCase() {
        if (this.aposCount % 2 == 0)
            this.aposCount++;

        return REMOVE_FIRST;
    }
%}

%%

/* Default Ignore */
.                                                                   { /* ignore */ }

{_}[\(\[\{\¿\¡]+{_}                                                 { return REMOVE_LAST; }
{_}[\,\.\?\!\:\;\\\%\}\]\)]+{_}                                     { return REMOVE_FIRST; }
{_}™{_}                                                             { return REMOVE_FIRST; }

{_}{QUOT}{_}                                                        { return this.quoteCount++ % 2 == 0 ? REMOVE_LAST : REMOVE_FIRST; }
{_}{APOS}{_}                                                        { return this.aposCount++ % 2 == 0 ? REMOVE_LAST : REMOVE_FIRST; }

{CJ}{_}{CJ}                                                         { return REMOVE_FIRST; }

/* Language Specific - English */

{_}{APOS}{LETTER}.                                                  { return REMOVE_FIRST; }
s{_}{APOS}{_}                                                       { return englishPossessiveCase(); }
{_}{CURRENCY}{_}                                                    { return REMOVE_LAST; }