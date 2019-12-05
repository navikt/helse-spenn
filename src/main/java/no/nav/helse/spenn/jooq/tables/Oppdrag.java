/*
 * This file is generated by jOOQ.
 */
package no.nav.helse.spenn.jooq.tables;


import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.processing.Generated;

import no.nav.helse.spenn.jooq.Indexes;
import no.nav.helse.spenn.jooq.Keys;
import no.nav.helse.spenn.jooq.Public;
import no.nav.helse.spenn.jooq.tables.records.OppdragRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row5;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


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
public class Oppdrag extends TableImpl<OppdragRecord> {

    private static final long serialVersionUID = 1813765930;

    /**
     * The reference instance of <code>public.oppdrag</code>
     */
    public static final Oppdrag OPPDRAG = new Oppdrag();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<OppdragRecord> getRecordType() {
        return OppdragRecord.class;
    }

    /**
     * The column <code>public.oppdrag.id</code>.
     */
    public final TableField<OppdragRecord, Long> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.BIGINT.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('oppdrag_id_seq'::regclass)", org.jooq.impl.SQLDataType.BIGINT)), this, "");

    /**
     * The column <code>public.oppdrag.sakskompleks_id</code>.
     */
    public final TableField<OppdragRecord, UUID> SAKSKOMPLEKS_ID = createField(DSL.name("sakskompleks_id"), org.jooq.impl.SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>public.oppdrag.created</code>.
     */
    public final TableField<OppdragRecord, Timestamp> CREATED = createField(DSL.name("created"), org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false).defaultValue(org.jooq.impl.DSL.field("now()", org.jooq.impl.SQLDataType.TIMESTAMP)), this, "");

    /**
     * The column <code>public.oppdrag.modified</code>.
     */
    public final TableField<OppdragRecord, Timestamp> MODIFIED = createField(DSL.name("modified"), org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false).defaultValue(org.jooq.impl.DSL.field("now()", org.jooq.impl.SQLDataType.TIMESTAMP)), this, "");

    /**
     * The column <code>public.oppdrag.utbetalingsreferanse</code>.
     */
    public final TableField<OppdragRecord, String> UTBETALINGSREFERANSE = createField(DSL.name("utbetalingsreferanse"), org.jooq.impl.SQLDataType.VARCHAR(30).nullable(false), this, "");

    /**
     * Create a <code>public.oppdrag</code> table reference
     */
    public Oppdrag() {
        this(DSL.name("oppdrag"), null);
    }

    /**
     * Create an aliased <code>public.oppdrag</code> table reference
     */
    public Oppdrag(String alias) {
        this(DSL.name(alias), OPPDRAG);
    }

    /**
     * Create an aliased <code>public.oppdrag</code> table reference
     */
    public Oppdrag(Name alias) {
        this(alias, OPPDRAG);
    }

    private Oppdrag(Name alias, Table<OppdragRecord> aliased) {
        this(alias, aliased, null);
    }

    private Oppdrag(Name alias, Table<OppdragRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> Oppdrag(Table<O> child, ForeignKey<O, OppdragRecord> key) {
        super(child, key, OPPDRAG);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.OPPDRAG_MODIFIED_IDX, Indexes.OPPDRAG_UTBETALINGSREFERANSE_KEY, Indexes.PK_OPPDRAG, Indexes.UQ_OPPDRAG_SAKSKOMPLEKS_ID);
    }

    @Override
    public Identity<OppdragRecord, Long> getIdentity() {
        return Keys.IDENTITY_OPPDRAG;
    }

    @Override
    public UniqueKey<OppdragRecord> getPrimaryKey() {
        return Keys.PK_OPPDRAG;
    }

    @Override
    public List<UniqueKey<OppdragRecord>> getKeys() {
        return Arrays.<UniqueKey<OppdragRecord>>asList(Keys.PK_OPPDRAG, Keys.UQ_OPPDRAG_SAKSKOMPLEKS_ID, Keys.OPPDRAG_UTBETALINGSREFERANSE_KEY);
    }

    @Override
    public Oppdrag as(String alias) {
        return new Oppdrag(DSL.name(alias), this);
    }

    @Override
    public Oppdrag as(Name alias) {
        return new Oppdrag(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Oppdrag rename(String name) {
        return new Oppdrag(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Oppdrag rename(Name name) {
        return new Oppdrag(name, null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<Long, UUID, Timestamp, Timestamp, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }
}