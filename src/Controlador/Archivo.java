/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controlador;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

/**
 *clase que permite construir las matrices, generar y cargar script.lp
 * @author User
 */
public class Archivo {
    
    //private BufferedReader nodos;
    private StringTokenizer stk;
    private double[][] matrizVentanaTiempos;
    private double[][] matrizTiemposDistancias;
    private int n;
    private int nodoInicial;
    private String scriptLp;
    private final String filename = "script.lp";
    private double fo;
    private double[] var;

    //funcion que permite construir las matrices a partir del archivo txt
    public void construirMatrices(FileReader fl) throws IOException {
        BufferedReader nodos = new BufferedReader(fl);
        n = Integer.parseInt(nodos.readLine());
        matrizVentanaTiempos = new double[n][3];
        matrizTiemposDistancias = new double[n][n];
        // tiempos de servicio y ventana de tiempo de casa sitio
        for (int i = 1; i <= n; i++) {
            stk = new StringTokenizer(nodos.readLine(), " ");
            int k = Integer.parseInt(stk.nextToken()) - 1;
            matrizVentanaTiempos[k][0] = Double.parseDouble(stk.nextToken());
            double ai = Double.parseDouble(stk.nextToken());
            double bi = Double.parseDouble(stk.nextToken());
            matrizVentanaTiempos[k][1] = ai;
            matrizVentanaTiempos[k][2] = bi;
            if ( ai == 0 && bi == 0)
                nodoInicial = i;
        }
        // distancia de un sitio i a un sitio j
        for (int i = 1; i <= n * (n - 1); i++) {
            stk = new StringTokenizer(nodos.readLine(), " ");
            matrizTiemposDistancias[Integer.parseInt(stk.nextToken()) - 1][Integer.parseInt(stk.nextToken()) - 1] = Double.parseDouble(stk.nextToken());
        }
    }
    
    //funcion que permite visualizar la matriz con las ventanas de tiempo
    public String getMatrizVentanaTiempos() {
        String salida = "Numero de sitios:  " + n + "\n\n";
        int i = 1;
        for (double[] matrizVentanaTiempo : matrizVentanaTiempos) {
            salida += "Ciudad #" + i + "\tTServicio:" + matrizVentanaTiempo[0] + "\tai" + matrizVentanaTiempo[1] + "\tbi" + matrizVentanaTiempo[2] + "\n";
        }
        return salida + "\n";
    }
    
    //funcion que permite visualizar la matriz de tiempos de desplazamiento TDij
    public String getMatrizTiemposDistancia() {
        String salida = "";
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    salida += "Tiempo de distancia entre ciudad #" + (i + 1) + " y ciudad #:" + (j + 1) + " --> " + matrizTiemposDistancias[i][j] + "\n";
                }
            }
        }
        return salida + "\n";
    }
    
    //funcion que permite crear el script.lp del modelo TSPTW
    public void generarScript(){     
        int m = n * 1000000;
        String fo = "min: t;";
        String restriccionCerradura1="";
        String restriccionCerradura2="";
        String restriccionCicloSum = "";
        String restriccionVentanas = "";
        String variablesBin="bin ";
        
        // contruccion de restriciones de cerradura, ciclo con M, ventanas de tiempo y variables binarias
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (i != j) {
                    restriccionCerradura1 += "x" + i + j;
                    restriccionCerradura2 += "x" + j + i;
                    if (j == nodoInicial) {
                        restriccionCicloSum += "t" + i + " + " + sumartDespltServicio(i, j) + " - " + m + " + " + m + " x" + i + j + " <= " + " t;\n";
                    }
                    else {
                        restriccionCicloSum += "t" + i + " + " + sumartDespltServicio(i, j) + " - " + m + " + " + m + " x" + i + j + " <= " + " t" + j + ";\n";
                    }
                    variablesBin += "x" + i + j;
                    if (j == n) {
                        restriccionCerradura1 += " = 1;\n";
                        restriccionCerradura2 += " = 1;\n";
                        variablesBin += ", ";
                    }
                    else if (i == n && j == n - 1) {
                        restriccionCerradura1 += " = 1;\n";
                        restriccionCerradura2 += " = 1;\n";
                        variablesBin += ";";
                    }
                    else {
                        restriccionCerradura1 += " + ";
                        restriccionCerradura2 += " + ";
                        variablesBin += ", ";
                    }
                }
            }
            restriccionCicloSum += "\n";
            restriccionVentanas += matrizVentanaTiempos[i - 1][1] + " <= t" + i + " <= " + matrizVentanaTiempos[i - 1][2] + ";\n";
        }
        scriptLp =  fo + "\n\n" + 
                    restriccionCerradura1 + "\n" + 
                    restriccionCerradura2 + "\n" + 
                    restriccionCicloSum   + "\n" +
                    restriccionVentanas   + "\n" +
                    variablesBin;
        write();
    }
    
    private double sumartDespltServicio (int i, int j) {
        return redondear(matrizTiemposDistancias[i - 1][j - 1] + matrizVentanaTiempos[i - 1][0]);
    }
    
    private void write(){

         File f = new File(filename);
         try{
             FileWriter w = new FileWriter(f);
             BufferedWriter bw = new BufferedWriter(w);
             PrintWriter wr = new PrintWriter(bw);
             wr.write(scriptLp);
             wr.close();
             bw.close();
         } catch(IOException e){
             
         }
    }
    
    //ejecutar un modelo existente en un archivo *.lp
    public void modelFromF(String filename) {
        try {
            LpSolve solver = LpSolve.readLp(filename, LpSolve.NORMAL, "TSPTW");
            // se resuelve el modelo
            solver.solve();
            
            // se obtienen los datos de la solucion
            fo = solver.getObjective();
            var = solver.getPtrVariables();
            
            // liberar memoria
            solver.deleteLp();
        }
        catch (LpSolveException e) {
            e.printStackTrace();
        }
    }

    public String getScriptLp() {
        return scriptLp;
    }
    
    public String getSolucion () {
        
            String resultado = "Funcion Objetivo min: t => " + redondear(fo) + "\n\n";
            String ruta = "";
            String tiempos = "";
            ArrayList ordenTiempos = new ArrayList<>();
            double[][] ordenRuta = new double[n][n];
            
            int k = 1;
            // se encuentran los nodos escogidos == 1
            for (int i = 1; i <= n; i++) {
                for (int j = 1; j <= n; j++, k++) {
                    if ( i != j && var[k] == 1)
                        ordenRuta[i - 1][j - 1] = var[k];
                    if (i == j)
                        k--;
                }
            }
            
            // se ordena la ruta con un puntero con la matriz ordenRuta
            int puntero = nodoInicial - 1;
            for (int i = 0; i < n; i++) {
                if (ordenRuta[puntero][i] == 1) {
                    ruta += "x" + (puntero + 1) + "-" + (i + 1) + " , ";
                    puntero = i;
                    i = 0;
                }
            } // agregamos el nodo que conecta al inicial
            ruta += "x" + (puntero + 1) + "->" + nodoInicial;
            
            // se capturan los tiempos y se ordenan de menor a mayor
            for (int i = 0; i < n; i++, k++)
                ordenTiempos.add(var[k]);
            Collections.sort(ordenTiempos);
            for (int i = 0; i < n; i++)
                tiempos += "x" + i + " => " + redondear((double) ordenTiempos.get(i)) + "    ";
            tiempos += "x" + n + " => " + redondear(fo) + "    ";
            
            return resultado += "Ruta de solucion:\n" + ruta + "\n\n" + "Tiempos por ciudad: \n" +  tiempos;
    }
    
    private double redondear (double num) {
        return Math.rint(num * 100) / 100;
    }
}
