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

%%

/* Default Ignore */
.                                                                   { /* ignore */ }

{_}\!                                                               { return REMOVE; }