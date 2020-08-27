package ca.bc.gov.educ.penreg.api.constants;

/**
 * <pre>
 *    AA	PEN is confirmed	The Submitted PEN was valid and confirmed	2
 *    B0	Valid CD but bad match	Check digit is valid on the Submitted PEN, but PEN appears to be for a different student. No matches are found using points formula. "B0" is changed to "F1" (the one match being the supplied PEN)	3
 *    B1	Valid CD but merged PEN	Check digit is valid on the Submitted PEN, but PEN is merged. True (correct) PEN is returned as possible match.	4
 *    BM	Valid CD but multiple matches Check digit is valid on the Submitted PEN, but PEN appears to be for another student. Multiple matches found using points formula. 5
 *    C0	New PEN created	Check digit is invalid on the Submitted PEN, or Submitted PEN is not on file. No matches are found using points formula. A brand new PEN is assigned.	6
 *    C1	PEN bad but 1 match by points	Check digit is invalid on the Submitted PEN, or PEN is not on file. One match is found using points formula.	7
 *    CM	PEN bad; M matches by points	Check digit is invalid on the Submitted PEN, or PEN is not on file.  Multiple matches were found using points formula.	8
 *    D0	No match found	No PEN was submitted. No matches found, brand new PEN assigned	9
 *    D1	Exactly one match found	No PEN was submitted. One and only one match found	10
 *    DM	Multiple matches found	No PEN was submitted. Multiple matches found using points formula	11
 *    F1	One questionable match	One questionable match found. A PEN may or may not have been submitted. 	12
 *    G0	Insufficient demog data	No PEN was submitted. Insufficient demographic data to perform pen processing when in update mode and the pen process has returned a "D0" code 13
 * </pre>
 */
public enum MatchAlgorithmStatusCode {
  AA,
  B0,
  B1,
  BM,
  C0,
  C1,
  CM,
  D0,
  D1,
  DM,
  F1,
  G0
}
