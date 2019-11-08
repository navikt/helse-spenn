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
import no.nav.helse.spenn.jooq.tables.records.OppdragstateRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row9;
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
public class Oppdragstate extends TableImpl<OppdragstateRecord> {

    private static final long serialVersionUID = -2049536140;

    /**
     * The reference instance of <code>public.oppdragstate</code>
     */
    public static final Oppdragstate OPPDRAGSTATE = new Oppdragstate();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<OppdragstateRecord> getRecordType() {
        return OppdragstateRecord.class;
    }

    /**
     * The column <code>public.oppdragstate.id</code>.
     */
    public final TableField<OppdragstateRecord, Long> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.BIGINT.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('oppdragstate_id_seq'::regclass)", org.jooq.impl.SQLDataType.BIGINT)), this, "");

    /**
     * The column <code>public.oppdragstate.sakskompleks_id</code>.
     */
    public final TableField<OppdragstateRecord, UUID> SAKSKOMPLEKS_ID = createField(DSL.name("sakskompleks_id"), org.jooq.impl.SQLDataType.UUID.nullable(false), this, "");

    /**
     * The column <code>public.oppdragstate.created</code>.
     */
    public final TableField<OppdragstateRecord, Timestamp> CREATED = createField(DSL.name("created"), org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false).defaultValue(org.jooq.impl.DSL.field("now()", org.jooq.impl.SQLDataType.TIMESTAMP)), this, "");

    /**
     * The column <code>public.oppdragstate.modified</code>.
     */
    public final TableField<OppdragstateRecord, Timestamp> MODIFIED = createField(DSL.name("modified"), org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false).defaultValue(org.jooq.impl.DSL.field("now()", org.jooq.impl.SQLDataType.TIMESTAMP)), this, "");

    /**
     * The column <code>public.oppdragstate.utbetalingsoppdrag</code>.
     */
    public final TableField<OppdragstateRecord, String> UTBETALINGSOPPDRAG = createField(DSL.name("utbetalingsoppdrag"), org.jooq.impl.SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.oppdragstate.status</code>.
     */
    public final TableField<OppdragstateRecord, String> STATUS = createField(DSL.name("status"), org.jooq.impl.SQLDataType.VARCHAR(16).nullable(false), this, "");

    /**
     * The column <code>public.oppdragstate.oppdragresponse</code>.
     */
    public final TableField<OppdragstateRecord, String> OPPDRAGRESPONSE = createField(DSL.name("oppdragresponse"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.oppdragstate.simuleringresult</code>.
     */
    public final TableField<OppdragstateRecord, String> SIMULERINGRESULT = createField(DSL.name("simuleringresult"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.oppdragstate.feilbeskrivelse</code>.
     */
    public final TableField<OppdragstateRecord, String> FEILBESKRIVELSE = createField(DSL.name("feilbeskrivelse"), org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * Create a <code>public.oppdragstate</code> table reference
     */
    public Oppdragstate() {
        this(DSL.name("oppdragstate"), null);
    }

    /**
     * Create an aliased <code>public.oppdragstate</code> table reference
     */
    public Oppdragstate(String alias) {
        this(DSL.name(alias), OPPDRAGSTATE);
    }

    /**
     * Create an aliased <code>public.oppdragstate</code> table reference
     */
    public Oppdragstate(Name alias) {
        this(alias, OPPDRAGSTATE);
    }

    private Oppdragstate(Name alias, Table<OppdragstateRecord> aliased) {
        this(alias, aliased, null);
    }

    private Oppdragstate(Name alias, Table<OppdragstateRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> Oppdragstate(Table<O> child, ForeignKey<O, OppdragstateRecord> key) {
        super(child, key, OPPDRAGSTATE);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.OPPDRAGSTATE_MODIFIED_IDX, Indexes.OPPDRAGSTATE_STATUS_IDX, Indexes.PK_OPPDRAGSTATE, Indexes.UQ_SAKSKOMPLEKS_ID);
    }

    @Override
    public Identity<OppdragstateRecord, Long> getIdentity() {
        return Keys.IDENTITY_OPPDRAGSTATE;
    }

    @Override
    public UniqueKey<OppdragstateRecord> getPrimaryKey() {
        return Keys.PK_OPPDRAGSTATE;
    }

    @Override
    public List<UniqueKey<OppdragstateRecord>> getKeys() {
        return Arrays.<UniqueKey<OppdragstateRecord>>asList(Keys.PK_OPPDRAGSTATE, Keys.UQ_SAKSKOMPLEKS_ID);
    }

    @Override
    public Oppdragstate as(String alias) {
        return new Oppdragstate(DSL.name(alias), this);
    }

    @Override
    public Oppdragstate as(Name alias) {
        return new Oppdragstate(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Oppdragstate rename(String name) {
        return new Oppdragstate(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Oppdragstate rename(Name name) {
        return new Oppdragstate(name, null);
    }

    // -------------------------------------------------------------------------
    // Row9 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row9<Long, UUID, Timestamp, Timestamp, String, String, String, String, String> fieldsRow() {
        return (Row9) super.fieldsRow();
    }
}
