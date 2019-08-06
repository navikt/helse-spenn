/*
 * This file is generated by jOOQ.
 */
package no.nav.helse.spenn.jooq.tables.records;


import java.sql.Timestamp;
import java.util.UUID;

import javax.annotation.Generated;

import no.nav.helse.spenn.jooq.tables.Oppdragstate;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record8;
import org.jooq.Row8;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.11"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class OppdragstateRecord extends UpdatableRecordImpl<OppdragstateRecord> implements Record8<Long, UUID, Timestamp, Timestamp, String, String, String, String> {

    private static final long serialVersionUID = 1636425180;

    /**
     * Setter for <code>public.oppdragstate.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.oppdragstate.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>public.oppdragstate.soknad_id</code>.
     */
    public void setSoknadId(UUID value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.oppdragstate.soknad_id</code>.
     */
    public UUID getSoknadId() {
        return (UUID) get(1);
    }

    /**
     * Setter for <code>public.oppdragstate.created</code>.
     */
    public void setCreated(Timestamp value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.oppdragstate.created</code>.
     */
    public Timestamp getCreated() {
        return (Timestamp) get(2);
    }

    /**
     * Setter for <code>public.oppdragstate.modified</code>.
     */
    public void setModified(Timestamp value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.oppdragstate.modified</code>.
     */
    public Timestamp getModified() {
        return (Timestamp) get(3);
    }

    /**
     * Setter for <code>public.oppdragstate.utbetalingsoppdrag</code>.
     */
    public void setUtbetalingsoppdrag(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.oppdragstate.utbetalingsoppdrag</code>.
     */
    public String getUtbetalingsoppdrag() {
        return (String) get(4);
    }

    /**
     * Setter for <code>public.oppdragstate.status</code>.
     */
    public void setStatus(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>public.oppdragstate.status</code>.
     */
    public String getStatus() {
        return (String) get(5);
    }

    /**
     * Setter for <code>public.oppdragstate.oppdragresponse</code>.
     */
    public void setOppdragresponse(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>public.oppdragstate.oppdragresponse</code>.
     */
    public String getOppdragresponse() {
        return (String) get(6);
    }

    /**
     * Setter for <code>public.oppdragstate.simuleringresult</code>.
     */
    public void setSimuleringresult(String value) {
        set(7, value);
    }

    /**
     * Getter for <code>public.oppdragstate.simuleringresult</code>.
     */
    public String getSimuleringresult() {
        return (String) get(7);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record8 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row8<Long, UUID, Timestamp, Timestamp, String, String, String, String> fieldsRow() {
        return (Row8) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row8<Long, UUID, Timestamp, Timestamp, String, String, String, String> valuesRow() {
        return (Row8) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field1() {
        return Oppdragstate.OPPDRAGSTATE.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UUID> field2() {
        return Oppdragstate.OPPDRAGSTATE.SOKNAD_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Timestamp> field3() {
        return Oppdragstate.OPPDRAGSTATE.CREATED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Timestamp> field4() {
        return Oppdragstate.OPPDRAGSTATE.MODIFIED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field5() {
        return Oppdragstate.OPPDRAGSTATE.UTBETALINGSOPPDRAG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field6() {
        return Oppdragstate.OPPDRAGSTATE.STATUS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field7() {
        return Oppdragstate.OPPDRAGSTATE.OPPDRAGRESPONSE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field8() {
        return Oppdragstate.OPPDRAGSTATE.SIMULERINGRESULT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component1() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID component2() {
        return getSoknadId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp component3() {
        return getCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp component4() {
        return getModified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component5() {
        return getUtbetalingsoppdrag();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component6() {
        return getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component7() {
        return getOppdragresponse();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component8() {
        return getSimuleringresult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value1() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID value2() {
        return getSoknadId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp value3() {
        return getCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp value4() {
        return getModified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value5() {
        return getUtbetalingsoppdrag();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value6() {
        return getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value7() {
        return getOppdragresponse();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value8() {
        return getSimuleringresult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OppdragstateRecord value1(Long value) {
        setId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OppdragstateRecord value2(UUID value) {
        setSoknadId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OppdragstateRecord value3(Timestamp value) {
        setCreated(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OppdragstateRecord value4(Timestamp value) {
        setModified(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OppdragstateRecord value5(String value) {
        setUtbetalingsoppdrag(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OppdragstateRecord value6(String value) {
        setStatus(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OppdragstateRecord value7(String value) {
        setOppdragresponse(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OppdragstateRecord value8(String value) {
        setSimuleringresult(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OppdragstateRecord values(Long value1, UUID value2, Timestamp value3, Timestamp value4, String value5, String value6, String value7, String value8) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached OppdragstateRecord
     */
    public OppdragstateRecord() {
        super(Oppdragstate.OPPDRAGSTATE);
    }

    /**
     * Create a detached, initialised OppdragstateRecord
     */
    public OppdragstateRecord(Long id, UUID soknadId, Timestamp created, Timestamp modified, String utbetalingsoppdrag, String status, String oppdragresponse, String simuleringresult) {
        super(Oppdragstate.OPPDRAGSTATE);

        set(0, id);
        set(1, soknadId);
        set(2, created);
        set(3, modified);
        set(4, utbetalingsoppdrag);
        set(5, status);
        set(6, oppdragresponse);
        set(7, simuleringresult);
    }
}