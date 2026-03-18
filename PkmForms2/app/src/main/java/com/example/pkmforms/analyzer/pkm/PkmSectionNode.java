package com.example.pkmforms.analyzer.pkm;

import java.util.ArrayList;

public class PkmSectionNode {
    public int          width       = 0;
    public int          height      = 0;
    public int          pointX      = 0;
    public int          pointY      = 0;
    public String       orientation = "VERTICAL";
    public PkmStyleNode style       = new PkmStyleNode();
    public ArrayList    elementos   = new ArrayList();
}