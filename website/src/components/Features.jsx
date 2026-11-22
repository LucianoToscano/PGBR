import SettingsGuide from './SettingsGuide.jsx';
import Gallery from './Gallery.jsx';
import { indexHref } from './guideData.js';

export default function Features() {
  return (
    <section className="section" id="features" aria-labelledby="features-heading">
      <div className="section__head">
        <p className="section__num">01 / Recursos</p>
        <h2 id="features-heading" className="section__title">
          Recursos.
        </h2>
        <p className="section__lead">
          Explore as configurações e veja como os recursos aparecem no aplicativo.
        </p>
      </div>

      <p className="guide-back"><a className="btn btn--primary" href={indexHref}>Ver todos os guias de recursos</a></p>
      <SettingsGuide />
      <Gallery />
    </section>
  );
}
