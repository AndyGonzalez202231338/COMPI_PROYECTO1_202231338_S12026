package com.example.pkmforms.analyzer.code;

import java.util.ArrayList;

public class IfNode {
    // Cada rama es Object[]{condicion, cuerpo}
    // La primera rama es el IF, las siguientes son ELSE IF, la ultima (sin condicion) es ELSE
    public ArrayList<Object[]> ramas = new ArrayList<>(); // {expresion, ArrayList<cuerpo>}
}