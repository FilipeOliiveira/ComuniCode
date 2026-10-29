import React from 'react';
import Icon from './Icon.jsx';
import MaterialCard from './MaterialCard.jsx';

export default function MaterialSection({ id, title, description, icon, items, onShowAll }) {
  if (!items.length) return null;
  return (
    <section id={id} className="mb-space-xl material-section" aria-labelledby={`${id}-heading`}>
      <div className="flex items-center justify-between mb-space-lg">
        <div className="flex items-center gap-space-sm min-w-0">
          <div className="w-8 h-8 shrink-0 rounded-lg bg-surface-container-highest text-primary flex items-center justify-center"><Icon name={icon} className="text-xl" /></div>
          <div>
            <h2 id={`${id}-heading`} className="font-headline-lg text-headline-lg text-on-surface">{title}</h2>
            <p className="font-body-sm text-body-sm text-on-surface-variant">{description}</p>
          </div>
        </div>
        {onShowAll && <a href="#acervo" onClick={onShowAll} className="inline-flex shrink-0 items-center gap-1 font-label-md text-label-md text-primary hover:text-primary-fixed">Ver todos<Icon name="arrow_forward" className="text-base" /></a>}
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-space-lg">
        {items.map(material => <MaterialCard key={material.id} material={material} />)}
      </div>
    </section>
  );
}
