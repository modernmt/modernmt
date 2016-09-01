/* Default Ignore */
.                                                                                                                        { /* ignore */ }

/* Number */
{Number}                                                                                                                 { return PROTECT; }
/* Percentage */
{Number}\%                                                                                                               { return PROTECT; }
/* Temperature */
{Number}(°F|°C|°K)                                                                                                       { return PROTECT; }

/* Dates */
[0-9]{1,4}(\/[0-9]{1,4}){0,2}                                                                                            { return PROTECT; }

/* URL */
((https?|HTTPS?)\:\/\/)?({UrlPart}\.)+{UrlPart}\/?({UrlPart}\/?)*(\.[:letter:]+)?(\?({UrlPart}(\={UrlPart})?\&?)*)?      { return PROTECT; }

/* E-mail */
([a-z0-9_\.-]+)@([\da-z\.-]+)\.([a-z\.]{2,6})                                                                            { return PROTECT; }

/* Acronym */
{Letter}\.({Letter}\.)+                                                                                                  { return PROTECT; }

/* &-separated entities */
{Letter}\&{Letter}                                                                                                       { return PROTECT; }

/* Pseudo HTML Entities */
\&#?[[:letter:][:digit:]\-\_]+;                                                                                          { return PROTECT; }

/* File extension */
\.{Letter}+                                                                                                              { return PROTECT; }

/* Period in middle of a sentence */
\.{_}[:lowercase:]                                                                                                       { yypushback(2); return PROTECT_ALL; }

/* Placeholders */
\_\_+                                                                                                                    { return PROTECT; }
\.\.+                                                                                                                    { return PROTECT; }

/* Names with apostrophe, like "O'Malley" */
{_}[A-Z]'[A-Z][:letter:]+                                                                                                { return PROTECT; }