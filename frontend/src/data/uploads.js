export const MAX_UPLOAD_BYTES = 100 * 1024 * 1024;
export const ACCEPTED_EXTENSIONS = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt', 'csv', 'zip', 'mp4', 'webm', 'mov', 'm4v', 'mp3', 'wav', 'ogg', 'png', 'jpg', 'jpeg', 'webp', 'gif'];
export const FILE_ACCEPT = ACCEPTED_EXTENSIONS.map(extension => `.${extension}`).join(',');

export function validateFile(file) {
  if (!file) return 'Selecione um arquivo para enviar.';
  if (file.size === 0) return 'O arquivo está vazio. Escolha outro arquivo.';
  if (file.size > MAX_UPLOAD_BYTES) return 'O arquivo deve ter no máximo 100 MB.';
  if (!ACCEPTED_EXTENSIONS.includes(file.name.split('.').pop().toLowerCase())) return 'Formato não suportado. Escolha um documento, vídeo, áudio, imagem ou ZIP.';
  return '';
}

export function fileDetails(file) {
  const extension = file.name.split('.').pop().toLowerCase();
  const mediaType = ['mp4', 'webm', 'mov', 'm4v'].includes(extension) ? 'video'
    : ['mp3', 'wav', 'ogg'].includes(extension) ? 'audio'
    : ['png', 'jpg', 'jpeg', 'webp', 'gif'].includes(extension) ? 'image' : 'document';
  return { filename: file.name, bytes: file.size, format: extension.toUpperCase(), mediaType };
}

async function openDatabase() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('comunicode-uploads', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('materials', { keyPath: 'material.id' });
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
    request.onblocked = () => reject(new Error('Feche outras abas do ComuniCode e tente novamente.'));
  });
}

export async function readUploads() {
  const database = await openDatabase();
  try {
    return await new Promise((resolve, reject) => {
      const request = database.transaction('materials').objectStore('materials').getAll();
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  } finally { database.close(); }
}

export async function saveUpload(material, file) {
  const database = await openDatabase();
  try {
    await new Promise((resolve, reject) => {
      const transaction = database.transaction('materials', 'readwrite');
      transaction.objectStore('materials').put({ material, file });
      transaction.oncomplete = resolve;
      transaction.onerror = () => reject(transaction.error);
      transaction.onabort = () => reject(transaction.error);
    });
  } finally { database.close(); }
}
