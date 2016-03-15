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

CJ         = [\u3100-\u312f\u3040-\u309F\u30A0-\u30FF\u31F0-\u31FF\u3300-\u337f\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff\uff65-\uff9f]
_          = " "
LETTER     = !(![:letter:]|{CJ})

DotInTheMiddle = \.{_}[:lowercase:]
UrlPart = [[:letter:][:digit:]\-\_]+
Url = ((https?|HTTPS?)\:\/\/)?({UrlPart}\.)+{UrlPart}\/?({UrlPart}\/?)*(\.[:letter:]+)?(\?({UrlPart}(\={UrlPart})?\&?)*)?
Acronym = {LETTER}\.({LETTER}\.)+
Number = ((\-|\+)?[0-9]+)?([\.,][0-9]+)*\%?
MultipleDots = \.\.+
FileExtension = \.{LETTER}+
Email = ([a-z0-9_\.-]+)@([\da-z\.-]+)\.([a-z\.]{2,6})

ProtectedPatterns = ((A\.)|(B\.)|(C\.)|(D\.)|(E\.)|(F\.)|(G\.)|(H\.)|(I\.)|(J\.)|(K\.)|(L\.)|(M\.)|(N\.)|(O\.)|(P\.)|(Q\.)|(R\.)|(S\.)|(T\.)|(U\.)|(V\.)|(W\.)|(X\.)|(Y\.)|(Z\.)|((A|a)(D|d)(J|j)\.)|((A|a)(D|d)(M|m)\.)|((A|a)(D|d)(V|v)\.)|((A|a)(M|m)(N|n)\.)|((A|a)(R|r)(C|c)(H|h)\.)|((A|a)(S|s)(S|s)(T|t)\.)|((A|a)(V|v)(V|v)\.)|((B|b)(A|a)(R|r)(T|t)\.)|((B|b)(C|c)(C|c)\.)|((B|b)(L|l)(D|d)(G|g)\.)|((B|b)(R|r)(I|i)(G|g)\.)|((B|b)(R|r)(O|o)(S|s)\.)|((C|c)(\.|\.)(A|a)(\.|\.)(P|p)\.)|((C|c)(\.|\.)(P|p)\.)|((C|c)(A|a)(P|p)(T|t)\.)|((C|c)(C|c)\.)|((C|c)(M|m)(D|d)(R|r)\.)|((C|c)(O|o)\.)|((C|c)(O|o)(L|l)\.)|((C|c)(O|o)(M|m)(D|d)(R|r)\.)|((C|c)(O|o)(N|n)\.)|((C|c)(O|o)(R|r)(P|p)\.)|((C|c)(P|p)(L|l)\.)|((D|d)(R|r)\.)|((D|d)(O|o)(T|t)(T|t)\.)|((D|d)(R|r)\.)|((D|d)(R|r)(S|s)\.)|((E|e)(G|g)(R|r)\.)|((E|e)(N|n)(S|s)\.)|((G|g)(E|e)(N|n)\.)|((G|g)(E|e)(O|o)(M|m)\.)|((G|g)(O|o)(V|v)\.)|((H|h)(O|o)(N|n)\.)|((H|h)(O|o)(S|s)(P|p)\.)|((H|h)(R|r)\.)|((I|i)(D|d)\.)|((I|i)(N|n)(G|g)\.)|((I|i)(N|n)(S|s)(P|p)\.)|((L|l)(T|t)\.)|((M|m)(M|m)\.)|((M|m)(R|r)\.)|((M|m)(R|r)(S|s)\.)|((M|m)(S|s)\.)|((M|m)(A|a)(J|j)\.)|((M|m)(E|e)(S|s)(S|s)(R|r)(S|s)\.)|((M|m)(L|l)(L|l)(E|e)\.)|((M|m)(M|m)(E|e)\.)|((M|m)(O|o)\.)|((M|m)(O|o)(N|n)(S|s)\.)|((M|m)(R|r)\.)|((M|m)(R|r)(S|s)\.)|((M|m)(S|s)\.)|((M|m)(S|s)(G|g)(R|r)\.)|((N|n)(\.|\.)(B|b)\.)|((O|o)(P|p)\.)|((O|o)(R|r)(D|d)\.)|((P|p)(\.|\.)(S|s)\.)|((P|p)(\.|\.)(T|t)\.)|((P|p)(F|f)(C|c)\.)|((P|p)(H|h)\.)|((P|p)(R|r)(O|o)(F|f)\.)|((P|p)(V|v)(T|t)\.)|((R|r)(P|p)\.)|((R|r)(S|s)(V|v)(P|p)\.)|((R|r)(A|a)(G|g)\.)|((R|r)(E|e)(P|p)\.)|((R|r)(E|e)(P|p)(S|s)\.)|((R|r)(E|e)(S|s)\.)|((R|r)(E|e)(V|v)\.)|((R|r)(I|i)(F|f)\.)|((R|r)(T|t)\.)|((S|s)(\.|\.)(A|a)\.)|((S|s)(\.|\.)(B|b)(\.|\.)(F|f)\.)|((S|s)(\.|\.)(P|p)(\.|\.)(M|m)\.)|((S|s)(\.|\.)(P|p)(\.|\.)(A|a)\.)|((S|s)(\.|\.)(R|r)(\.|\.)(L|l)\.)|((S|s)(E|e)(N|n)\.)|((S|s)(E|e)(N|n)(S|s)\.)|((S|s)(F|f)(C|c)\.)|((S|s)(G|g)(T|t)\.)|((S|s)(I|i)(G|g)\.)|((S|s)(I|i)(G|g)(G|g)\.)|((S|s)(O|o)(C|c)\.)|((S|s)(P|p)(E|e)(T|t)(T|t)\.)|((S|s)(R|r)\.)|((S|s)(T|t)\.)|((S|s)(U|u)(P|p)(T|t)\.)|((S|s)(U|u)(R|r)(G|g)\.)|((V|v)(\.|\.)(P|p)\.)|((A|a)(\.|\.)(C|c)\.)|((A|a)(C|c)(C|c)\.)|((A|a)(L|l)(L|l)\.)|((B|b)(A|a)(N|n)(C|c)\.)|((C|c)(\.|\.)(A|a)\.)|((C|c)(\.|\.)(C|c)(\.|\.)(P|p)\.)|((C|c)(\.|\.)(M|m)\.)|((C|c)(\.|\.)(P|p)\.)|((C|c)(\.|\.)(S|s)\.)|((C|c)(\.|\.)(V|v)\.)|((C|c)(O|o)(R|r)(R|r)\.)|((D|d)(O|o)(T|t)(T|t)\.)|((E|e)(\.|\.)(P|p)(\.|\.)(C|c)\.)|((E|e)(C|c)(C|c)\.)|((E|e)(S|s)\.)|((F|f)(A|a)(T|t)(T|t)\.)|((G|g)(G|g)\.)|((I|i)(N|n)(T|t)\.)|((L|l)(E|e)(T|t)(T|t)\.)|((O|o)(G|g)(G|g)\.)|((O|o)(N|n)\.)|((P|p)(\.|\.)(C|c)\.)|((P|p)(\.|\.)(C|c)(\.|\.)(C|c)\.)|((P|p)(\.|\.)(E|e)(S|s)\.)|((P|p)(\.|\.)(F|f)\.)|((P|p)(\.|\.)(R|r)\.)|((P|p)(\.|\.)(V|v)\.)|((P|p)(O|o)(S|s)(T|t)\.)|((P|p)(P|p)\.)|((R|r)(A|a)(C|c)(C|c)\.)|((R|r)(I|i)(C|c)\.)|((S|s)(\.|\.)(N|n)(\.|\.)(C|c)\.)|((S|s)(E|e)(G|g)\.)|((S|s)(G|g)(G|g)\.)|((S|s)(S|s)\.)|((T|t)(E|e)(L|l)\.)|((U|u)(\.|\.)(S|s)\.)|((V|v)(\.|\.)(R|r)\.)|((V|v)(\.|\.)(S|s)\.)|(v\.)|((V|v)(S|s)\.)|((I|i)(\.|\.)(E|e)\.)|((R|r)(E|e)(V|v)\.)|((E|e)(\.|\.)(G|g)\.)|((N|n)(O|o)(S|s)\.)|((N|n)(R|r)\.))
NumericProtectedPatters = (((N|n)(O|o)\.)|((A|a)(R|r)(T|t)\.)|((P|p)(P|p)\.))

ItalianContractions = [:letter:]\'[:letter:]

%%

/* Default Ignore */
.                                                                   { /* ignore */ }

{Number}                                                            { return PROTECT; }
{Url}                                                               { return PROTECT; }
{Email}                                                             { return PROTECT; }
{Acronym}                                                           { return PROTECT; }
{MultipleDots}                                                      { return PROTECT; }
{FileExtension}                                                     { return PROTECT; }
{DotInTheMiddle}                                                    { yypushback(2); return PROTECT_ALL; }

{ItalianContractions}                                               { yypushback(1); return PROTECT; }
[^[:letter:]]{ProtectedPatterns}[^[:letter:]] { zzStartReadOffset = 1; yypushback(1); return PROTECT; }
[^[:letter:]]{NumericProtectedPatters}{_}[:digit:] { zzStartReadOffset = 1; yypushback(2); return PROTECT; }