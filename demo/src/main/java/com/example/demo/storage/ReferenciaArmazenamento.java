package com.example.demo.storage;

/**
 * Referencia permanente gravada em anexo.url para arquivos hospedados pelo portal:
 * {@code storage://<bucket>/<caminho>}. Nunca e exposta ao navegador.
 */
public record ReferenciaArmazenamento(String bucket, String caminho) {
    public static final String PREFIXO = "storage://";

    public ReferenciaArmazenamento {
        if (bucket == null || bucket.isBlank() || caminho == null || caminho.isBlank()) {
            throw new IllegalArgumentException("Referencia de armazenamento invalida.");
        }
    }

    public static boolean ehInterna(String url) { return url != null && url.startsWith(PREFIXO); }

    public static ReferenciaArmazenamento de(String url) {
        if (!ehInterna(url)) throw new IllegalArgumentException("Referencia de armazenamento invalida.");
        String resto = url.substring(PREFIXO.length());
        int barra = resto.indexOf('/');
        if (barra <= 0) throw new IllegalArgumentException("Referencia de armazenamento invalida.");
        return new ReferenciaArmazenamento(resto.substring(0, barra), resto.substring(barra + 1));
    }

    @Override
    public String toString() { return PREFIXO + bucket + "/" + caminho; }
}
