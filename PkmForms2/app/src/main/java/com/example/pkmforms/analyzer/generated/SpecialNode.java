package com.example.pkmforms.analyzer.generated;

import java.util.ArrayList;

public class SpecialNode {
    public String    nombre      = "";   // nombre de la variable
    public String    tipo        = "";   // "OPEN" | "DROP" | "SELECT" | "MULTIPLE"
    public Object    width       = null; // Double, String (identificador) o "?" (comodin)
    public Object    height      = null;
    public String    label       = "";
    public ArrayList options     = new ArrayList();
    public Object    correct     = null;
    public int       comodines   = 0;   // cantidad de comodines en esta definicion
    public StyleNode style       = null;
}