import i18next from "i18next";
import XHR from 'i18next-xhr-backend';

i18next
  .use(XHR)
  .init({
    lngs: [ "ja", "en" ],
    fallbackLng: "ja",
    backend: {
      loadPath: "/assets/locales/{{lng}}/{{ns}}.json"
    }
  });

export default i18next;
