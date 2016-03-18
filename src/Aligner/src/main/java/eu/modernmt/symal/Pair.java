/*
 * Copyright (C) 2010 Miquel Esplà Gomis
 *
 * author: Miquel Esplà Gomis
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package eu.modernmt.symal;

/**
 * This class contains a pair of variables which can be of the same type or of
 * different ones.
 * @author Miquel Esplà Gomis
 * @version 1.0
 */
public class Pair<T,R> {
  /** First element of the pair */
  private T first;
  /** Second element of the pari */
  private R second;

  /**
   * Overloaded constructor for the class.
   * @param first New value for the {@link #first} element of the pair.
   * @param second New value for the {@link #second} element of the pair.
   */
  public Pair(T first, R second) {
      this.first = first;
      this.second = second;
  }

  /**
   * Method that compares two elements of any type.
   * @param first Firts element to be compared.
   * @param second Second element to be compared.
   * @return Returns <code>true</code> if both elements of the pair are the
   * same in both pairs which are being compared.
   */
  public static boolean same(Object first, Object second) {
    return first == null ? second == null : first.equals(second);
  }

  /**
   * Method which returns the first element of the pair.
   * @return Returns the value of the first element of the pair.
   */
  public T getFirst()
  {
      return first;
  }

  /**
   * Method which returns the second element of the pair.
   * @return Returns the value of the second element of the pair.
   */
  public R getSecond(){
      return second;
  }

  /**
   * Method that sets the value of the first element in the pair.
   * @param first New value for the first element in the pair.
   */
  public void setFirst(T first){
      this.first = first;
  }


  /**
   * Method that sets the value of the second element in the pair.
   * @param second New value for the second element in the pair.
   */
  public void setSecond(R second) {
      this.second = second;
  }

  
  /**
   * Method that compares the instance of the class with another one.
   * @param obj Object with which the instance will be compared.
   * @return Returns <code>true</code> if both elements of the pair are the
   * same in both pairs which are being compared.
   */
  @Override
  public boolean equals(Object obj) {
    if( ! (obj instanceof Pair))
      return false;
    Pair p = (Pair)obj;
    return same(p.first, this.first) && same(p.second, this.second);
  }

  /**
   * Method that computes a hash code for an instance of the class.
   * @return Returns the computed hash code.
   */
  @Override
  public int hashCode() {
      int hash = 7;
      hash = 79 * hash + (this.first != null ? this.first.hashCode() : 0);
      hash = 79 * hash + (this.second != null ? this.second.hashCode() : 0);
      return hash;
  }
}
