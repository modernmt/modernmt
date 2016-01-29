package eu.modernmt.processing.tokenizer.jflex;

%%

%public
%class EnglishAnnotator
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
WHITESPACE = \r\n | [ \r\n\t\f]
LETTER     = !(![:letter:]|{CJ})

DotInTheMiddle = \.{WHITESPACE}[[:lowercase:]|\-]
UrlPart = [[:letter:][:digit:]\-\_]+
Url = (https?\:\/\/)?({UrlPart}\.)+{UrlPart}\/?({UrlPart}\/?)*(\.[:letter:]+)?(\?({UrlPart}(\={UrlPart})?\&?)*)?
Acronym = {LETTER}\.({LETTER}\.)+
Number = ((\-|\+)?[0-9]+)?([\.,][0-9]+)*\%?
MultipleDots = \.\.+
FileExtension = \.{LETTER}+
Email = ([a-z0-9_\.-]+)@([\da-z\.-]+)\.([a-z\.]{2,6})

/*
 * Language specific MACRO
 */

ProtectedPatterns = ([A-Z]\.|"Dept\."|"Adj\."|"Adm\."|"Adv\."|"Asst\."|"Bart\."|"Bldg\."|"Brig\."|"Bros\."|"Capt\."|"Cmdr\."|"Col\."|"Comdr\."|"Con\."|"Corp\."|"Cpl\."|"DR\."|"Dr\."|"Drs\."|"Ens\."|"Gen\."|"Gov\."|"Hon\."|"Hr\."|"Hosp\."|"Insp\."|"Lt\."|"MM\."|"MR\."|"MRS\."|"MS\."|"Maj\."|"Messrs\."|"Mlle\."|"Mme\."|"Mr\."|"Mrs\."|"Ms\."|"Msgr\."|"Op\."|"Ord\."|"Pfc\."|"Ph\."|"Prof\."|"Pvt\."|"Rep\."|"Reps\."|"Res\."|"Rev\."|"Rt\."|"Sen\."|"Sens\."|"Sfc\."|"Sgt\."|"Sr\."|"St\."|"Supt\."|"Surg\."|"v\."|"vs\."|"i\.e\."|"rev\."|"Nos\."|"Nr\."|"Jan\."|"Feb\."|"Mar\."|"Apr\."|"Jun\."|"Jul\."|"Aug\."|"Sep\."|"Oct\."|"Nov\."|"Dec\.")
NumericProtectedPatters = ("Art\."|"No\."|"pp\.")
EnglishContractions = {LETTER}\'{LETTER}
PossessiveCase = \'"s"{WHITESPACE}


%%

{Number}                                                            { return PROTECT; }
{Url}                                                               { return PROTECT; }
{Email}                                                             { return PROTECT; }
{Acronym}                                                           { return PROTECT; }
{MultipleDots}                                                      { return PROTECT; }
{FileExtension}                                                     { return PROTECT; }
{DotInTheMiddle}                                                    { yypushback(2); return PROTECT_ALL; }

/*
 * Language specific rules
 */

[^[:letter:]]{ProtectedPatterns}[^[:letter:]]                       { zzStartReadOffset = 1; yypushback(1); return PROTECT; }
^{ProtectedPatterns}[^[:letter:]]                                   { yypushback(1); return PROTECT; }
[^[:letter:]]{NumericProtectedPatters}{WHITESPACE}[:digit:]         { zzStartReadOffset = 1; yypushback(2); return PROTECT; }
^{NumericProtectedPatters}{WHITESPACE}[:digit:]                     { yypushback(2); return PROTECT; }

{EnglishContractions}                                               { yypushback(1); return PROTECT_RIGHT; }
{PossessiveCase}                                                    { yypushback(1); return PROTECT; }


/** Ignore the rest */
.                                                                   { /* ignore */ }