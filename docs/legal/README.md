# DotShredzilla terms of use

The canonical terms are maintained here in five languages. Each document has
the same eleven sections: provider/scope, app functions, training/health/safety,
accounts, rest timer/notifications, Pro/purchases/tips, privacy/advertising,
account deletion, service changes, open-source licensing/mandatory rights, and
changes/contact.

| Language | Source | Public HTML route |
| --- | --- | --- |
| English | [terms.en.md](terms.en.md) | `/en/dotshredzilla/terms` |
| العربية | [terms.ar.md](terms.ar.md) | `/ar/dotshredzilla/terms` |
| Deutsch | [terms.de.md](terms.de.md) | `/de/dotshredzilla/terms` |
| Français | [terms.fr.md](terms.fr.md) | `/fr/dotshredzilla/terms` |
| Русский | [terms.ru.md](terms.ru.md) | `/ru/dotshredzilla/terms` |

The routes are hosted by the separate DotSuite website. The documents do not
replace the [privacy policy](https://dotsuite.dev/en/privacy#dotshredzilla) or
[account-deletion request](https://dotsuite.dev/en/privacy#delete-dotshredzilla).

## Website synchronization

After changing the documents, export the bundle used by DotSuite:

```sh
python3 scripts/export-terms.py --output /path/to/dotsuite/src/content/dotshredzilla-terms.json
```

The exporter requires all five complete eleven-section documents and copies their
text unchanged. DotSuite bundles the JSON in its server-rendered public page;
visitors do not need a GitHub connection or a DotSuite account to read it.
Publish both repositories through their normal GitHub workflows. Website
updates do not change an already signed Android bundle.

Keep the translations aligned when changing obligations or operational
commitments. Section 3 must keep stating that the app is not medical advice
and that suggestions are general; do not add blanket liability exclusions.
In-app deletion is immediate; the 30-day commitment applies to email requests
and starts after account-ownership verification, as the privacy policy explains.

Publishing a page is distinct from obtaining any required acceptance from
existing app users. These documents preserve rights granted by Apache 2.0 and
mandatory consumer rights; they do not certify compliance in every country.
