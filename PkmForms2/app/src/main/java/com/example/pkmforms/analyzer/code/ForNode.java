package com.example.pkmforms.analyzer.code;

import java.util.ArrayList;

public class ForNode {
    // FOR (var = init ; cond ; var = paso) { }
    public boolean  esRango   = false;
    public String   variable  = "";
    public Object   inicio    = null;   // expresion valor inicial
    public Object   condicion = null;   // expresion condicion (clasico) / fin de rango
    public String   varPaso   = "";     // variable del paso (clasico)
    public Object   paso      = null;   // expresion del paso (clasico)
    // Modo "rango": FOR (i in inicio .. fin) { }
    public Object   fin       = null;   // expresion fin del rango
    public ArrayList cuerpo   = new ArrayList();
}