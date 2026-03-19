package com.example.pkmforms.analyzer.pkm;

import java.util.ArrayList;

public class PkmSelectNode {
    public String       label    = "";
    public int          width    = 0;
    public int          height   = 0;
    public ArrayList    opciones = new ArrayList();
    public int          correct  = -1;
    public PkmStyleNode style    = new PkmStyleNode();
}