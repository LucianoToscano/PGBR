import { cooldown } from '../content.js';

// Community cooldown guidance: a short explainer, two expandable action lists,
// and a compact distance/wait tables. All values are framed as estimates.
export default function Cooldown() {
  return (
    <section className="section" id="cooldown" aria-labelledby="cooldown-heading">
      <div className="section__head">
        <p className="section__num">02 / Tempo de espera</p>
        <h2 id="cooldown-heading" className="section__title">
          Tempo de espera entre locais.
        </h2>
        <p className="section__lead">{cooldown.intro}</p>
      </div>

      <div className="cooldown__groups">
        <details className="cooldown__group" open>
          <summary>
            <span>Inicia a espera</span>
            <span className="faq__mark" aria-hidden="true" />
          </summary>
          <ul className="cooldown__list cooldown__list--trigger">
            {cooldown.triggers.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </details>

        <details className="cooldown__group">
          <summary>
            <span>Não inicia a espera</span>
            <span className="faq__mark" aria-hidden="true" />
          </summary>
          <ul className="cooldown__list cooldown__list--safe">
            {cooldown.safe.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </details>
      </div>

      <figure className="cooldown__chart">
        <figcaption className="cooldown__chart-cap">
          Distância entre ações e tempo estimado de espera. Leia cada coluna de cima para baixo.
        </figcaption>
        <div className="cooldown__tables">
          {[cooldown.chart.slice(0, 15), cooldown.chart.slice(15)].map((rows, index) => (
            <div className="cooldown__table-wrap" key={index}>
              <table className="cooldown__table">
                <caption className="visually-hidden">Distância e espera estimada, parte {index + 1} de 2. Leia cada tabela de cima para baixo.</caption>
                <thead><tr><th scope="col">Distância (km)</th><th scope="col">Espera (est.)</th></tr></thead>
                <tbody>{rows.map(row => (
                  <tr key={row.km}><th scope="row">{row.km}</th><td>{row.wait}</td></tr>
                ))}</tbody>
              </table>
            </div>
          ))}
        </div>
      </figure>

      <p className="cooldown__note">
        O cronômetro do PGBR faz uma estimativa local, não lê o servidor. Esperar não garante o sucesso das ações.
      </p>
    </section>
  );
}
