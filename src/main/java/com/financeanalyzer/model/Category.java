package com.financeanalyzer.model;

public enum Category {
    CASA_UTENZE_ASSICURAZIONI(TipoMovimento.USCITA),
    SPESA_QUOTIDIANA(TipoMovimento.USCITA),
    TRASPORTI(TipoMovimento.USCITA),
    SALUTE(TipoMovimento.USCITA),
    SHOPPING_TEMPO_LIBERO(TipoMovimento.USCITA),
    TASSE_COMMISSIONI(TipoMovimento.USCITA),
    TRASFERIMENTI_PRELIEVI(null),
    STIPENDIO_COMPENSI(TipoMovimento.ENTRATA),
    ALTRE_ENTRATE(TipoMovimento.ENTRATA);

    /* ********************************************************************************************** */

    private final TipoMovimento tipoAssociato;

    /* ********************************************************************************************** */

    Category(TipoMovimento tipoAssociato) {
        this.tipoAssociato = tipoAssociato;
    }

    public boolean isCompatibileCon(TipoMovimento tipoMovimento) {
        return tipoAssociato == null || tipoAssociato == tipoMovimento;
    }
}