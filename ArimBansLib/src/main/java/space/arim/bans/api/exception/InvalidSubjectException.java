/* 
 * ArimBansLib, an API for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansLib. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api.exception;

import space.arim.bans.api.Subject;

public class InvalidSubjectException extends InternalStateException {

	private static final long serialVersionUID = 5864870266394935646L;

	/**
     * Constructs an <code>InvalidSubjectException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
	public InvalidSubjectException(String s) {
		super(s);
	}
	
	/**
     * Constructs an <code>InvalidSubjectException</code> with the
     * specified detail message and cause
     *
     * @param   s      the detail message.
     * @param   cause  the cause
     */
	public InvalidSubjectException(String s, Exception cause) {
		super(s, cause);
	}

	/**
	 * Constructs an <code>InvalidSubjectException</code> for the
	 * given subject. Assumes subject type is missing.
	 * 
	 * @param subj - the subject whose type is missing.
	 */
	public InvalidSubjectException(Subject subj) {
		super("Subject type is completely missing for " + subj.toString());
	}
}
