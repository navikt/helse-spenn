/*
 * This file is generated by jOOQ.
 */
package no.nav.helse.spenn.jooq;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import no.nav.helse.spenn.jooq.tables.Avstemming;
import no.nav.helse.spenn.jooq.tables.Oppdragstate;

import org.jooq.Catalog;
import org.jooq.Sequence;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.2"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Public extends SchemaImpl {

    private static final long serialVersionUID = -2048788138;

    /**
     * The reference instance of <code>public</code>
     */
    public static final Public PUBLIC = new Public();

    /**
     * The table <code>public.avstemming</code>.
     */
    public final Avstemming AVSTEMMING = no.nav.helse.spenn.jooq.tables.Avstemming.AVSTEMMING;

    /**
     * The table <code>public.oppdragstate</code>.
     */
    public final Oppdragstate OPPDRAGSTATE = no.nav.helse.spenn.jooq.tables.Oppdragstate.OPPDRAGSTATE;

    /**
     * No further instances allowed
     */
    private Public() {
        super("public", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Sequence<?>> getSequences() {
        List result = new ArrayList();
        result.addAll(getSequences0());
        return result;
    }

    private final List<Sequence<?>> getSequences0() {
        return Arrays.<Sequence<?>>asList(
            Sequences.AVSTEMMING_ID_SEQ,
            Sequences.OPPDRAGSTATE_ID_SEQ);
    }

    @Override
    public final List<Table<?>> getTables() {
        List result = new ArrayList();
        result.addAll(getTables0());
        return result;
    }

    private final List<Table<?>> getTables0() {
        return Arrays.<Table<?>>asList(
            Avstemming.AVSTEMMING,
            Oppdragstate.OPPDRAGSTATE);
    }
}
