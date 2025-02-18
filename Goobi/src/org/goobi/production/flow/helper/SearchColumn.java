package org.goobi.production.flow.helper;
/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - https://goobi.io
 *          - https://www.intranda.com 
 *          - https://github.com/intranda/goobi
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
public class SearchColumn {

    private String value = "";

    private int order;

    public SearchColumn(int order) {
        this.order = order;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTableName() {
        if (value == null || value.isEmpty()) {
            return "";
        } else if (value.startsWith("prozesse.")) {
            return "prozesse";
        } else if (value.startsWith("projekte.")) {
            return "projekte";
        } else if (value.startsWith("prozesseeigenschaften.")) {
            return "prozesseeigenschaften" + order;
        } else if (value.startsWith("vorlageneigenschaften.")) {
            return "vorlageneigenschaften" + order;
        } else if (value.startsWith("werkstueckeeigenschaften.")) {
            return "werkstueckeeigenschaften" + order;
        } else if (value.startsWith("metadata.")) {
            return "metadata" + order;
        }
        return "";
    }

    public int getOrder() {
        return order;
    }

    public String getTableType() {
        if (value == null || value.isEmpty() || value.startsWith("prozesse.")) {
            return "";
        } else if (value.startsWith("projekte.")) {
            return "projekte ";
        } else if (value.startsWith("prozesseeigenschaften.")) {
            return "prozesseeigenschaften ";
        } else if (value.startsWith("vorlageneigenschaften.")) {

            return "vorlageneigenschaften ";
        } else if (value.startsWith("werkstueckeeigenschaften.")) {
            return "werkstueckeeigenschaften ";
        } else if (value.startsWith("metadata.")) {
            return "metadata ";
        }
        return "";
    }

    public String getColumnName() {
        if (value == null || value.isEmpty() || !value.contains(".")) {
            return "";
        } else if (value.startsWith("prozesse.") || value.startsWith("projekte.")) {
            return value.substring(value.indexOf(".") + 1);
        } else if (value.startsWith("metadata.")) {
            return "print";
        } else {
            return "Wert";
        }
    }

    public String getJoinClause() {

        if (getTableName().isEmpty()) {
            return "";
        }
        if (value.startsWith("prozesseeigenschaften.")) {
            return " prozesseeigenschaften " + getTableName() + " ON prozesse.ProzesseID = " + getTableName() + ".prozesseID AND " + getTableName() + ".Titel = \""
                 + value.substring(value.indexOf(".") + 1) + "\"";
//            return " prozesse.ProzesseID = " + getTableName() + ".prozesseID AND " + getTableName() + ".Titel = \""
//                    + value.substring(value.indexOf(".") + 1) + "\"";
        } else if (value.startsWith("metadata.")) {
            return " metadata " + getTableName() + " ON prozesse.ProzesseID = " + getTableName() + ".processid AND " + getTableName() + ".name = \""
                    + value.substring(value.indexOf(".") + 1) + "\"";
//            return " prozesse.ProzesseID = " + getTableName() + ".processID AND " + getTableName() + ".name = \""
//                    + value.substring(value.indexOf(".") + 1) + "\"";
        } else if (value.startsWith("projekte.")) {
            return " projekte " + getTableName() + " ON prozesse.ProjekteID = " + getTableName() + ".ProjekteID";
//            return " prozesse.ProjekteID = " + getTableName() + ".ProjekteID";
        } else if (value.startsWith("vorlageneigenschaften.")) {
            return "vorlagen vorlagen" + order + " ON prozesse.ProzesseID = vorlagen"+ order + ".ProzesseID LEFT JOIN vorlageneigenschaften " + getTableName()
                    + " ON " + getTableName()+".vorlagenID = vorlagen" + order + ".vorlagenID AND " + getTableName() + ".Titel =\"" + value.substring(value.indexOf(".") + 1) + "\"";
//            return " prozesse.ProzesseID = vorlagen" + order + ".ProzesseID AND vorlagen" + order + ".VorlagenID = " + getTableName()
//                    + ".vorlagenID AND " + getTableName() + ".Titel =\"" + value.substring(value.indexOf(".") + 1) + "\"";
        } else if (value.startsWith("werkstueckeeigenschaften.")) {
            return "werkstuecke werkstuecke" + order + " ON prozesse.ProzesseID = werkstuecke"+ order + ".ProzesseID LEFT JOIN werkstueckeeigenschaften " + getTableName()
                   + " ON " + getTableName()+".werkstueckeID = werkstuecke" + order + ".WerkstueckeID AND " + getTableName() + ".Titel =\"" + value.substring(value.indexOf(".") + 1) + "\"";
//            return " prozesse.ProzesseID = werkstuecke" + order + ".ProzesseID AND werkstuecke" + order + ".WerkstueckeID = " + getTableName()
//                    + ".werkstueckeID AND " + getTableName() + ".Titel =\"" + value.substring(value.indexOf(".") + 1) + "\"";
        }

        return "";
    }

    public String getAdditionalTable() {
        if (value.startsWith("werkstueckeeigenschaften.")) {
            return " werkstuecke werkstuecke" + order;
        } else if (value.startsWith("vorlageneigenschaften.")) {
            return " vorlagen vorlagen" + order;
        }
        return "";
    }
}
