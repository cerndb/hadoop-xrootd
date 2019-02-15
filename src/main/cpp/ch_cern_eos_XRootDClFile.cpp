/*
 * Copyright 2014-2022 CERN IT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <iostream>
#include <string>
#include "stdlib.h"

#include "ch_cern_eos_XRootDClFile.h"
#include "XrdCl/XrdClFile.hh"
#include "XrdCl/XrdClDefaultEnv.hh"

int Xrd_debug = 0;

extern "C" {
    /*
     * Class:     ch_cern_eos_XRootDClFile
     * Method:    initFile
     * Signature: ()J
     */
    JNIEXPORT jlong JNICALL Java_ch_cern_eos_XRootDClFile_initFile(JNIEnv *, jobject) {

	// Andreas, 24.8.2016, doubled 31.8.2016, reduced to 540 (600s hadoop timeouts) 5.9.2016:
	XrdCl::Env* xenv = XrdCl::DefaultEnv::GetEnv();
	xenv->PutInt("RequestTimeout", 540);
	xenv->PutInt("StreamErrorWindow",540);
	xenv->PutInt("StreamTimeout",540);

	XrdCl::File *file = new XrdCl::File();

	// Giuseppe 7.9.2016 (for castor)
	file->SetProperty("ReadRecovery", "false");
	file->SetProperty("WriteRecovery", "false");


	char *dd = getenv("Xrd_debug");
	if (dd) Xrd_debug = 1;

	if (Xrd_debug) printf("initFile: File structure created, addr=%p\n", file);
	return (jlong) file;
	

    };

    /*
     * Class:     ch_cern_eos_XRootDClFile
     * Method:    disposeFile
     * Signature: ()J
     */
    JNIEXPORT jlong JNICALL Java_ch_cern_eos_XRootDClFile_disposeFile (JNIEnv *env, jobject This, jlong handle) {
	XrdCl::File *file = (XrdCl::File *) handle;

	if (Xrd_debug) printf("disposeFile: deleting %p\n", file);

	delete file;
	return 0L;

    };

    /*
     * Class:     ch_cern_eos_XRootDClFile
     * Method:    openFile
     * Signature: (Ljava/lang/String;II)J
     */
    JNIEXPORT jlong JNICALL Java_ch_cern_eos_XRootDClFile_openFile (JNIEnv *env, jobject This, jlong handle, jstring url_p, jint flags, jint mode) {
	uint16_t timeout = 0;
	const char *fn = env->GetStringUTFChars(url_p, 0);

	if (Xrd_debug) std::cout << "openFile " << handle << ": '" << fn << "'\n";

	std::string Url(fn);
	env->ReleaseStringUTFChars(url_p, fn);
	
	XrdCl::OpenFlags::Flags oflags = static_cast<XrdCl::OpenFlags::Flags>(flags);
	XrdCl::Access::Mode acc = static_cast<XrdCl::Access::Mode>(mode);

	if (Xrd_debug) {
	    std::cout << "openFile: flags " << oflags << " accessmode " << std::oct << acc << "\n";
	}

	XrdCl::File *file = (XrdCl::File *) handle;
	XrdCl::XRootDStatus status = file->Open(Url, oflags, acc, timeout);
	if (status.status != 0) std::cout << "openFile "<< Url << ": status " << status.ToString() << "\n";

	return *(jlong *) &status;

    };

    /*
     * Class:     ch_cern_eos_XRootDClFile
     * Method:    readFile
     * Signature: (J[BJI)J
     */
    JNIEXPORT jlong JNICALL Java_ch_cern_eos_XRootDClFile_readFile (JNIEnv *env, jobject This, jlong handle, jlong filepos, jbyteArray b, jint off, jint len) {
	jboolean isCopy;
	
	char *buf = (char *)env->GetPrimitiveArrayCritical(b, &isCopy);
	if (isCopy) printf("readFile: isCopy = %d\n", isCopy);

	XrdCl::File *file = (XrdCl::File *) handle;

	uint32_t bytesRead;
	uint16_t timeout = 0;


	XrdCl::XRootDStatus status = file->Read(filepos, len, buf+off, bytesRead, timeout);
	if (Xrd_debug) std::cout << "readFile " << handle << ": status " << status.ToStr() << " bytes read " << bytesRead << "\n";

	env->ReleasePrimitiveArrayCritical(b, buf, isCopy);

	if (bytesRead >= 0) {
	    if (Xrd_debug)
		printf("XrdClFile.Read %ld return %d bytes\n", handle, bytesRead);
	    return (long) bytesRead;
	}

	long code = *(long *) &status;
	if (Xrd_debug || status.status != 0)
		printf("XrdClFile.Read %ld return 0x%li status %s\n", handle, code, status.ToStr().c_str());

	return -code;
    };

/*
 * Class:     ch_cern_eos_XRootDClFile
 * Method:    writeFile
 * Signature: (JJ[BII)J
 */
    JNIEXPORT jlong JNICALL Java_ch_cern_eos_XRootDClFile_writeFile (JNIEnv *env, jobject This, jlong handle, jlong filepos, jbyteArray b, jint off, jint len) {
	uint64_t offset = filepos;
	uint32_t size = len;
	uint16_t timeout = 900;
	jboolean isCopy;
	XrdCl::File *file = (XrdCl::File *) handle;
	
	char *buf = (char *)env->GetPrimitiveArrayCritical(b, &isCopy);
	if (isCopy) printf("writeFile: isCopy = %d\n", isCopy);

	void *buffer = (buf+off);

	XrdCl::XRootDStatus status = file->Write(offset, size, buffer, timeout);
	if (Xrd_debug || (status.status != 0))
		std::cout << "writeFile " << handle << ": " << status.ToStr() << " filepos " << filepos << " len " << len << " off " << off << "\n";

	env->ReleasePrimitiveArrayCritical(b, buf, isCopy);

	return *(jlong *) &status;
    };

/*
 * Class:     ch_cern_eos_XRootDClFile
 * Method:    closeFile
 * Signature: (J)J
 */
    JNIEXPORT jlong JNICALL Java_ch_cern_eos_XRootDClFile_closeFile (JNIEnv *env, jobject This, jlong handle) {
	XrdCl::File *file = (XrdCl::File *) handle;

	uint16_t timeout = 900;	    /* eos: replica sync on close, might take longer than default 300s */

	XrdCl::XRootDStatus status = file->Close(timeout);
	if (Xrd_debug || status.status != 0) 
	    std::cout << "closeFile " << handle << ": status " << status.ToString() <<  "\n";

	/* delete file will be handled through "dispose" of the java object */

	return *(jlong *) &status;
    };

/*
 * Class:     ch_cern_eos_XRootDClFile
 * Method:    syncFile
 * Signature: (J)J
 */
    JNIEXPORT jlong JNICALL Java_ch_cern_eos_XRootDClFile_syncFile (JNIEnv *env, jobject This, jlong handle) {
	XrdCl::File *file = (XrdCl::File *) handle;
	uint16_t timeout = 900;	    /* eos: replica sync, might take longer than default 300s */

	XrdCl::XRootDStatus status = file->Sync(timeout);
	if (Xrd_debug || status.status != 0) 
	    std::cout << "syncFile " << handle << ": status " << status.ToString() <<  "\n";

	return *(jlong *) &status;

    };
}
