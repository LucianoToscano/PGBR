import { steps, faqs, links } from '../content.js';
import { ArrowIcon } from './Icons.jsx';

export default function GetStarted() {
  return (
    <section className="section" id="get-started" aria-labelledby="get-started-heading">
      <div className="section__head">
        <p className="section__num">03 / Primeiros passos</p>
        <h2 id="get-started-heading" className="section__title">
          Três passos para a primeira sessão.
        </h2>
        <p className="section__lead">
          Cada versão inclui histórico de alterações, checksum SHA-256 e informações da compilação.
        </p>
      </div>

      <ol className="steps">
        {steps.map((step) => (
          <li className="step" key={step.n}>
            <span className="step__num" aria-hidden="true">{step.n}</span>
            <h3 className="step__title">{step.title}</h3>
            <p className="step__body">{step.body}</p>
          </li>
        ))}
      </ol>

      <div className="cta">
        <a className="btn btn--primary" href={links.releasesLatest}>
          Ver compilações de teste
          <ArrowIcon />
        </a>
        {/* Link do projeto de origem temporariamente oculto, mantendo os créditos legais. */}
      </div>

      <p className="cta__release">
        <span className="cta__badge">Teste</span>
        A compilação <a href={links.releasesCurrent}>PGBR-v0.00.03-LT</a> é experimental e inclui o instalador Root opcional.
      </p>

      <div className="faq">
        <h3 className="faq__heading">Informações importantes</h3>
        {faqs.map((item) => (
          <details className="faq__item" key={item.q}>
            <summary>
              <span>{item.q}</span>
              <span className="faq__mark" aria-hidden="true" />
            </summary>
            <p>{item.a}</p>
          </details>
        ))}
      </div>
    </section>
  );
}
