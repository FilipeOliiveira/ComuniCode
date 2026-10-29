import React from 'react';

const paths = {
  search: 'M21 21l-5-5 M10.5 3a7.5 7.5 0 1 0 0 15 7.5 7.5 0 0 0 0-15',
  logout: 'M9 3H4v18h5 M10 12h11 M17 8l4 4-4 4',
  explore: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20 M16 8l-3 5-5 3 3-5z',
  folder_shared: 'M3 5h6l2 2h10v13H3z M8 12h8 M8 16h5',
  auto_stories: 'M12 5v16 M12 5C8 2 4 3 2 4v15c3-1 7-1 10 2 3-3 7-3 10-2V4c-2-1-6-2-10 1',
  search_off: 'M21 21l-5-5 M10.5 3a7.5 7.5 0 1 0 0 15 7.5 7.5 0 0 0 0-15 M8 8l5 5 M13 8l-5 5',
  calendar_today: 'M4 5h16v16H4z M4 10h16 M8 3v4 M16 3v4',
  open_in_new: 'M14 3h7v7 M21 3L10 14 M10 3H3v18h18v-7',
  arrow_forward: 'M4 12h16 M14 6l6 6-6 6',
};

export default function Icon({ name, className = '' }) {
  return <svg className={`ui-icon ${className}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>;
}
