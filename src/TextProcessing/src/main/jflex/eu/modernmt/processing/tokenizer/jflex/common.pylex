/* Default Ignore */
.                                                                   { /* ignore */ }

{Number}                                                            { return PROTECT; }
{Url}                                                               { return PROTECT; }
{Email}                                                             { return PROTECT; }
{Acronym}                                                           { return PROTECT; }
{MultipleDots}                                                      { return PROTECT; }
{FileExtension}                                                     { return PROTECT; }
{DotInTheMiddle}                                                    { yypushback(2); return PROTECT_ALL; }