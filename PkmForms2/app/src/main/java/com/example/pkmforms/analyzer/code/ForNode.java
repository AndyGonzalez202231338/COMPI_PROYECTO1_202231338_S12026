package com.example.pkmforms.analyzer.code;

import java.util.ArrayList;

public class ForNode {
    public boolean  esRango   = false;
    public String   variable  = "";
    public Object   inicio    = null;
    public Object   condicion = null;
    public String   varPaso   = "";
    public Object   paso      = null;
    public Object   fin       = null;
    public ArrayList cuerpo   = new ArrayList();
    public int      linea     = 0;
}