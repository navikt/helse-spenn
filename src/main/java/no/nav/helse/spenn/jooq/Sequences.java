/*
 * This file is generated by jOOQ.
 */
package no.nav.helse.spenn.jooq;


import javax.annotation.processing.Generated;

import org.jooq.Sequence;
import org.jooq.impl.SequenceImpl;


/**
 * Convenience access to all sequences in public
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.1"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Sequences {

    /**
     * The sequence <code>public.avstemming_id_seq</code>
     */
    public static final Sequence<Long> AVSTEMMING_ID_SEQ = new SequenceImpl<Long>("avstemming_id_seq", Public.PUBLIC, org.jooq.impl.SQLDataType.BIGINT.nullable(false));

    /**
     * The sequence <code>public.oppdragstate_id_seq</code>
     */
    public static final Sequence<Long> OPPDRAGSTATE_ID_SEQ = new SequenceImpl<Long>("oppdragstate_id_seq", Public.PUBLIC, org.jooq.impl.SQLDataType.BIGINT.nullable(false));
}
