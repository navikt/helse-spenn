/*
 * This file is generated by jOOQ.
 */
package no.nav.helse.spenn.jooq.tables.records;


import java.sql.Timestamp;
import java.util.UUID;

import javax.annotation.processing.Generated;

import no.nav.helse.spenn.jooq.tables.Oppdrag;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


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
public class OppdragRecord extends UpdatableRecordImpl<OppdragRecord> implements Record5<Long, UUID, Timestamp, Timestamp, String> {

    private static final long serialVersionUID = 2119993326;

    /**
     * Setter for <code>public.oppdrag.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.oppdrag.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>public.oppdrag.sakskompleks_id</code>.
     */
    public void setSakskompleksId(UUID value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.oppdrag.sakskompleks_id</code>.
     */
    public UUID getSakskompleksId() {
        return (UUID) get(1);
    }

    /**
     * Setter for <code>public.oppdrag.created</code>.
     */
    public void setCreated(Timestamp value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.oppdrag.created</code>.
     */
    public Timestamp getCreated() {
        return (Timestamp) get(2);
    }

    /**
     * Setter for <code>public.oppdrag.modified</code>.
     */
    public void setModified(Timestamp value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.oppdrag.modified</code>.
     */
    public Timestamp getModified() {
        return (Timestamp) get(3);
    }

    /**
     * Setter for <code>public.oppdrag.utbetalingsreferanse</code>.
     */
    public void setUtbetalingsreferanse(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.oppdrag.utbetalingsreferanse</code>.
     */
    public String getUtbetalingsreferanse() {
        return (String) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<Long, UUID, Timestamp, Timestamp, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<Long, UUID, Timestamp, Timestamp, String> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return Oppdrag.OPPDRAG.ID;
    }

    @Override
    public Field<UUID> field2() {
        return Oppdrag.OPPDRAG.SAKSKOMPLEKS_ID;
    }

    @Override
    public Field<Timestamp> field3() {
        return Oppdrag.OPPDRAG.CREATED;
    }

    @Override
    public Field<Timestamp> field4() {
        return Oppdrag.OPPDRAG.MODIFIED;
    }

    @Override
    public Field<String> field5() {
        return Oppdrag.OPPDRAG.UTBETALINGSREFERANSE;
    }

    @Override
    public Long component1() {
        return getId();
    }

    @Override
    public UUID component2() {
        return getSakskompleksId();
    }

    @Override
    public Timestamp component3() {
        return getCreated();
    }

    @Override
    public Timestamp component4() {
        return getModified();
    }

    @Override
    public String component5() {
        return getUtbetalingsreferanse();
    }

    @Override
    public Long value1() {
        return getId();
    }

    @Override
    public UUID value2() {
        return getSakskompleksId();
    }

    @Override
    public Timestamp value3() {
        return getCreated();
    }

    @Override
    public Timestamp value4() {
        return getModified();
    }

    @Override
    public String value5() {
        return getUtbetalingsreferanse();
    }

    @Override
    public OppdragRecord value1(Long value) {
        setId(value);
        return this;
    }

    @Override
    public OppdragRecord value2(UUID value) {
        setSakskompleksId(value);
        return this;
    }

    @Override
    public OppdragRecord value3(Timestamp value) {
        setCreated(value);
        return this;
    }

    @Override
    public OppdragRecord value4(Timestamp value) {
        setModified(value);
        return this;
    }

    @Override
    public OppdragRecord value5(String value) {
        setUtbetalingsreferanse(value);
        return this;
    }

    @Override
    public OppdragRecord values(Long value1, UUID value2, Timestamp value3, Timestamp value4, String value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached OppdragRecord
     */
    public OppdragRecord() {
        super(Oppdrag.OPPDRAG);
    }

    /**
     * Create a detached, initialised OppdragRecord
     */
    public OppdragRecord(Long id, UUID sakskompleksId, Timestamp created, Timestamp modified, String utbetalingsreferanse) {
        super(Oppdrag.OPPDRAG);

        set(0, id);
        set(1, sakskompleksId);
        set(2, created);
        set(3, modified);
        set(4, utbetalingsreferanse);
    }
}