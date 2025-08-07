module com.typerr {
    requires javafx.controls;
    requires java.prefs;
    requires java.sql;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.typerr.database to com.fasterxml.jackson.databind;
    opens com.typerr.network to com.fasterxml.jackson.databind;
    
    exports com.typerr;
    exports com.typerr.database;
    exports com.typerr.charts;
    exports com.typerr.ui;
    exports com.typerr.network;
    exports com.typerr.statics;
}