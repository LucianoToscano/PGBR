import { links } from '../content.js';
import { asset } from '../assets.js';
import { homeHref, indexHref } from './guideData.js';

export default function Footer({ onGuide = false }) {
  return (
    <footer className="site-footer">
      <div className="site-footer__inner">
        <div className="site-footer__top">
          <a className="brand" href={onGuide ? homeHref : '#top'}>
            <img className="brand__mark" src={asset('logo.webp')} alt="" width="26" height="26" />
            <span className="brand__name">PGBR</span>
          </a>
          <nav className="site-footer__links" aria-label="Rodapé">
            <a href={indexHref}>Guias de recursos</a>
            <a href={links.github}>GitHub</a>
            <a href={links.releasesLatest}>Releases</a>
            
            <a href={links.kofi}>Licenças</a>
            <a href={`${links.github}/blob/main/THIRD_PARTY.md`}>Créditos</a>
          </nav>
        </div>
        <p className="site-footer__legal">
          O PGBR é baseado em um projeto de código aberto sob licença MIT. Os créditos originais e
          as licenças de terceiros são preservados. O PGBR é independente e não possui vínculo
          com os responsáveis por Pokémon GO. Pokémon e seus recursos pertencem aos respectivos titulares.
        </p>
        <p className="site-footer__legal">Imagem do mapa © <a href="https://www.openstreetmap.org/copyright">colaboradores do OpenStreetMap</a>.</p>
      </div>
    </footer>
  );
}
