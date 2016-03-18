/* Default Ignore */
.                                                                   { /* ignore */ }

{_}[\(\[\{\¿\¡]+{_}                                                 { return REMOVE_LAST; }
{_}[\,\.\?\!\:\;\\\%\}\]\)]+{_}                                     { return REMOVE_FIRST; }

{_}{quot}{_}                                                        { return this.quoteCount++ % 2 == 0 ? REMOVE_LAST : REMOVE_FIRST; }
{_}{apos}{_}                                                        { return this.aposCount++ % 2 == 0 ? REMOVE_LAST : REMOVE_FIRST; }

{CJK}{_}{CJK}                                                       { return REMOVE_FIRST; }