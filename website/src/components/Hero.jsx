import { links, heroFacts } from '../content.js';
import { asset } from '../assets.js';
import { ArrowIcon } from './Icons.jsx';
import RouteMotif from './RouteMotif.jsx';

export default function Hero() {
  return (
    <section className="hero" id="top">
      <RouteMotif className="hero__motif" />
      <div className="hero__inner">
        <div className="hero__copy">
          <p className="eyebrow">
            <span className="eyebrow__dot" aria-hidden="true" />
            Projeto independente e de código aberto
          </p>
          <h1 className="hero__title">
            Sessões mais práticas.{' '}
            <span className="accent-text">Tudo em um menu.</span>
          </h1>
          <p className="hero__lead">
            Controles de localização, capturas mais rápidas e automações opcionais para Pokémon GO.
            Uma versão para Android ARM64 e emulador MuMu, sem necessidade de Root.
          </p>
          <div className="hero__actions">
            <a className="btn btn--primary" href={links.releasesLatest}>
              Baixar versão mais recente
              <ArrowIcon />
            </a>
            <a className="btn btn--ghost" href={links.github}>
              Ver código no GitHub
            </a>
          </div>
          <dl className="hero__facts">
            {heroFacts.map((fact) => (
              <div className="hero__fact" key={fact.term}>
                <dt>{fact.term}</dt>
                <dd>{fact.detail}</dd>
              </div>
            ))}
          </dl>
        </div>

        <figure className="hero__shot">
          <img
            src={asset('images/encounter.png')}
            alt="Sobreposições do PGBR em um encontro no Pokémon GO: PC, nível, IV, frutas, transferência e indicador do VGP."
            width="1080"
            height="2404"
            loading="eager"
            fetchPriority="high"
            decoding="async"
          />
          <figcaption className="shot-tag">
            <span className="shot-tag__num" aria-hidden="true">01</span>
            Informações exibidas durante um encontro
          </figcaption>
        </figure>
      </div>
    </section>
  );
}
