import { useEffect, useRef, useState } from 'react';
import { readUploads, saveUpload } from '../data/uploads.js';

export default function useUploads() {
  const [uploads, setUploads] = useState([]);
  const [storageError, setStorageError] = useState('');
  const urls = useRef([]);

  useEffect(() => {
    let active = true;
    readUploads().then(records => {
      if (!active) return;
      const saved = records.map(record => {
        const objectUrl = URL.createObjectURL(record.file);
        urls.current.push(objectUrl);
        return { ...record.material, objectUrl };
      });
      setUploads(previous => [...previous, ...saved.filter(item => !previous.some(existing => existing.id === item.id))].sort((a, b) => new Date(b.uploadedAt) - new Date(a.uploadedAt)));
    }).catch(() => { if (active) setStorageError('Não foi possível carregar os materiais salvos neste navegador.'); });
    return () => {
      active = false;
      urls.current.forEach(url => URL.revokeObjectURL(url));
      urls.current = [];
    };
  }, []);

  async function addUpload(material, file) {
    await saveUpload(material, file);
    const objectUrl = URL.createObjectURL(file);
    urls.current.push(objectUrl);
    setUploads(previous => [{ ...material, objectUrl }, ...previous.filter(item => item.id !== material.id)]);
    setStorageError('');
  }

  return { uploads, addUpload, storageError };
}
