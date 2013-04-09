/**
 * Copyright (C) 2012-2013, Markus Sprunck
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - The name of its contributor may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.sw_engineering_candies.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

class ScaReportUtility {

	private static final String EMPTY = "";

	private static final Logger LOGGER = Logger.getLogger(ScaReportUtility.class);

	private static void run(final String xslt, final String input, final String output, final String param,
			final String value) {

		FileOutputStream outputStream = null;
		try {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(input + "  > " + xslt + " " + param + " " + value + " >  " + output);
			}

			// Process the Source into a Transformer Object
			System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
			final TransformerFactory transformerFactory = TransformerFactory.newInstance();
			final InputStream inputStream = ScaReportUtility.class
					.getResourceAsStream("/com/sw_engineering_candies/xsl/" + xslt);
			final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			final StreamSource source = new StreamSource(reader);
			final Transformer transformer = transformerFactory.newTransformer(source);

			// Add a parameter for the transformation
			if (!param.isEmpty()) {
				transformer.setParameter(param, value);
			}

			outputStream = new FileOutputStream(output);
			final StreamResult outputTarget = new StreamResult(outputStream);
			final StreamSource xmlSource = new StreamSource(input);

			// Transform the XML Source to a Result
			transformer.transform(xmlSource, outputTarget);

		} catch (final TransformerConfigurationException e) {
			LOGGER.error(e.getMessage());
		} catch (final TransformerException e) {
			LOGGER.error(e.getMessage());
		} catch (final FileNotFoundException e) {
			LOGGER.error(e.getMessage());
		} finally {
			if (null != outputStream) {
				try {
					outputStream.close();
				} catch (final IOException e) {
					LOGGER.error(e.getMessage());
				}
			}
		}
	}

	static void deletefile(final String pathName) {
		final File file = new File(pathName);
		if (!file.delete()) {
			LOGGER.error("Unable to delete file " + pathName);
		}
	}

	public static void main(final String[] args) {

		// Prepare userDirectory and tempDirectoryPrefix
		final String currentDirectory = System.getProperty("user.dir");
		final String userDirectory = currentDirectory.replace('\\', '/') + '/';
		final String timeStamp = Integer.toHexString((int) System.nanoTime());
		final String tempDirectory = System.getProperty("java.io.tmpdir");
		final String tempDirectoryPrefix = tempDirectory.replace('\\', '/') + timeStamp;

		// 1. Create intermediate xml-file for Findbugs
		final String inputFileFindbugs = userDirectory + "findbugs.xml";
		final String findbugsTempFile = tempDirectoryPrefix + "_PostFB.xml";
		run("prepare_findbugs.xslt", inputFileFindbugs, findbugsTempFile, EMPTY, EMPTY);

		// 2. Create intermediate xml-file for Checkstyle
		final String inputFileCheckstyle = userDirectory + "checkstyle.xml";
		final String checkstyleTempFile = tempDirectoryPrefix + "_PostCS.xml";
		run("prepare_checkstyle.xslt", inputFileCheckstyle, checkstyleTempFile, EMPTY, EMPTY);

		// 3. Create intermediate xml-file for PMD
		final String inputFilePMD = userDirectory + "pmd.xml";
		final String pmdTempFile = tempDirectoryPrefix + "_PostPM.xml";
		run("prepare_pmd.xslt", inputFilePMD, pmdTempFile, EMPTY, EMPTY);

		// 4. Merge first two files and create firstMergeResult file
		final String firstMergeResult = tempDirectoryPrefix + "_FirstMerge.xml";
		run("merge.xslt", checkstyleTempFile, firstMergeResult, "with", findbugsTempFile);

		// 5. Merge result file with third file and create secondMergeResult
		// file
		final String secondMergeResult = tempDirectoryPrefix + "_SecondMerge.xml";
		run("merge.xslt", firstMergeResult, secondMergeResult, "with", pmdTempFile);

		// 6. Create html report out of secondMergeResult
		final String htmlOutputFileName = userDirectory + "result.html";
		run("create_html.xslt", secondMergeResult, htmlOutputFileName, EMPTY, EMPTY);

		// Delete all temporary files
		deletefile(findbugsTempFile);
		deletefile(checkstyleTempFile);
		deletefile(pmdTempFile);
		deletefile(firstMergeResult);
		deletefile(secondMergeResult);
	}
}
