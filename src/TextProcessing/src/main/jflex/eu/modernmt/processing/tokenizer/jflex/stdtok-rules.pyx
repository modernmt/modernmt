/* Default Ignore */
.                                                                                                                        { /* ignore */ }

/* Number */
((\-|\+)?[0-9]+)?([\.,][0-9]+)*\%?                                                                                       { return PROTECT; }

/* URL */
((https?|HTTPS?)\:\/\/)?({UrlPart}\.)+{UrlPart}\/?({UrlPart}\/?)*(\.[:letter:]+)?(\?({UrlPart}(\={UrlPart})?\&?)*)?      { return PROTECT; }

/* E-mail */
([a-z0-9_\.-]+)@([\da-z\.-]+)\.([a-z\.]{2,6})                                                                            { return PROTECT; }

/* Acronym */
{Letter}\.({Letter}\.)+                                                                                                  { return PROTECT; }

/* Multiple dots */
\.\.+                                                                                                                    { return PROTECT; }

/* File extension */
\.{Letter}+                                                                                                              { return PROTECT; }

/* Period in middle of a sentence */
\.{_}[:lowercase:]                                                                                                       { yypushback(2); return PROTECT_ALL; }