#define dbg 0
#include <iostream>
#include <string>
#include <stdlib.h>
#include <stdio.h>

#include "ch_cern_eos_XrdClFile.h"
#include "xrootd/XrdCl/XrdClFile.hh"
#include "xrootd/XrdCl/XrdClFileSystem.hh"
#include "ch_cern_eos_EOSFileSystem.h"

#ifdef __cplusplus
extern "C" {
#endif
#undef ch_cern_eos_EOSFileSystem_SHUTDOWN_HOOK_PRIORITY
#define ch_cern_eos_EOSFileSystem_SHUTDOWN_HOOK_PRIORITY 10L


int EOS_debug = 0;

/*
 * Class:     ch_cern_eos_EOSFileSystem
 * Method:    initFileSystem
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ch_cern_eos_EOSFileSystem_initFileSystem (JNIEnv *env, jobject This, jstring url_p) {

	const char *urlstr = env->GetStringUTFChars(url_p, 0);

	std::string urlS(urlstr);
	XrdCl::URL url(urlS);

	char *dd = getenv("EOS_debug");
	if (dd) EOS_debug = 1;

	if (EOS_debug) {
	    std::cout << "initFileSystem urlS " << urlS << "\n";
	    std::cout << "initFileSystem url " << url.GetURL() << "\n";
	}

	XrdCl::FileSystem *fs = new XrdCl::FileSystem(url);

	env->ReleaseStringUTFChars(url_p, urlstr);
	
	return (jlong) fs;
};


/*
 * Class:     ch_cern_eos_EOSFileSystem
 * Method:    getFileStatusS
 * Signature: (Ljava/lang/String;)Lorg/apache/hadoop/fs/FileStatus;
 */
JNIEXPORT jobject JNICALL Java_ch_cern_eos_EOSFileSystem_getFileStatusS (JNIEnv *env, jobject This, jlong handle, jstring url_p, jobject path) {
	XrdCl::FileSystem *fs = (XrdCl::FileSystem *) handle;
	const char *fn = env->GetStringUTFChars(url_p, 0);
	if (EOS_debug) {
	    std::cout << "getFileStatusS fs handle " << handle << "\n";
	    printf("getFileStatusS: '%s'\n", fn);
	}

	uint16_t timeout = 0;

	XrdCl::StatInfo *resp = 0;
	XrdCl::XRootDStatus status = fs->Stat(fn, resp, timeout);

	env->ReleaseStringUTFChars(url_p, fn);

	if (EOS_debug) std::cout << "getFileStatusS: status" << status.ToString() << "\n";

	if (status.status != 0) return NULL;

	if (EOS_debug) std::cout << resp->TestFlags(resp->IsDir) << " " << resp->GetSize() << " " << resp->GetModTime() << "\n";

	jclass cls = env->FindClass("org/apache/hadoop/fs/FileStatus");
	jmethodID methodID = env->GetMethodID(cls, "<init>", "(JZIJJLorg/apache/hadoop/fs/Path;)V");

	if (EOS_debug) {
	    std::cout << "cls " << cls << "\n";
	    std::cout << "methodID " << methodID << "\n";
	}	

	jobject st = env->NewObject(cls, methodID, resp->GetSize(), resp->TestFlags(resp->IsDir), 1, 256*1024L, resp->GetModTime()*1000, path );

	if (EOS_debug) std::cout << "st " << *(long *) &st << "\n";

	return st;

};

/*
 * Class:     ch_cern_eos_EOSFileSystem
 * Method:    Rm
 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jlong JNICALL Java_ch_cern_eos_EOSFileSystem_Rm (JNIEnv *env, jobject This, jlong handle, jstring fn_p) {
	XrdCl::FileSystem *fs = (XrdCl::FileSystem *) handle;
	const char *fn = env->GetStringUTFChars(fn_p, 0);
	if (EOS_debug) {
	    std::cout << "Rm fs handle " << handle << "\n";
	    printf("delete: '%s'\n", fn);
	}

	uint16_t timeout = 0;

	XrdCl::XRootDStatus status = fs->Rm(fn, timeout);
	
#if 0
	 if (status.status == 1 && status.code == 400 && status.errNo == 3016) {
	    if (EOS_debug) 
	    std::cout << "retry RmDir " << status.status <<" "<<status.code<<" "<<status.errNo<< "\n";
	    status = fs->RmDir(fn, timeout);
	}
#endif

	if (EOS_debug)
	    std::cout << "Rm " << fn << " " << handle << ": status " <<status.status<<" "<<status.code<<" "<<status.errNo<<" "<< status.ToString() <<  "\n";
	env->ReleaseStringUTFChars(fn_p, fn);

	return *(jlong *) &status;

};

/*
 * Class:     ch_cern_eos_EOSFileSystem
 * Method:    RmDir
 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jlong JNICALL Java_ch_cern_eos_EOSFileSystem_RmDir (JNIEnv *env, jobject This, jlong handle, jstring fn_p) {

	XrdCl::FileSystem *fs = (XrdCl::FileSystem *) handle;
	const char *fn = env->GetStringUTFChars(fn_p, 0);
	if (EOS_debug) {
	    std::cout << "RmDir fs handle " << handle << "\n";
	    printf("delete: '%s'\n", fn);
	}

	uint16_t timeout = 0;

	XrdCl::XRootDStatus status = fs->RmDir(fn, timeout);

	if (EOS_debug) std::cout << "RmDir " << fn << " " << handle << ": status " << status.ToString() <<  "\n";
	env->ReleaseStringUTFChars(fn_p, fn);

	return *(jlong *) &status;

};

/*
 * Class:     ch_cern_eos_EOSFileSystem
 * Method:    Mv
 * Signature: (JLjava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jlong JNICALL Java_ch_cern_eos_EOSFileSystem_Mv (JNIEnv *env, jobject This, jlong handle, jstring src_p, jstring dst_p) {
	XrdCl::FileSystem *fs = (XrdCl::FileSystem *) handle;
	const char *src = env->GetStringUTFChars(src_p, 0);
	const char *dst = env->GetStringUTFChars(dst_p, 0);
	uint16_t timeout = 0;

	XrdCl::XRootDStatus status = fs->Mv(src, dst, timeout);
	env->ReleaseStringUTFChars(src_p, src);
	env->ReleaseStringUTFChars(dst_p, dst);
	if (EOS_debug) {
	    std::cout << "Mv fs handle " << handle << "\n";
	    printf("rename '%s' '%s' status = 0x%x\n", src, dst, status.ToString().c_str());
	}


	return *(jlong *) &status;
};

/*
 * Class:     ch_cern_eos_EOSFileSystem
 * Method:    MkDir
 * Signature: (JLjava/lang/String;S)I
 */
JNIEXPORT jlong JNICALL Java_ch_cern_eos_EOSFileSystem_MkDir (JNIEnv *env, jobject This, jlong handle, jstring dirname_p, jshort mode_p) {
	XrdCl::FileSystem *fs = (XrdCl::FileSystem *) handle;
	const char *dirname = env->GetStringUTFChars(dirname_p, 0);
	uint16_t timeout = 0;

	XrdCl::Access::Mode mode = static_cast<XrdCl::Access::Mode>(mode_p);
	if (EOS_debug) 
	    std::cout << "MkDir mode " << mode << "\n";


	XrdCl::XRootDStatus status = fs->MkDir(dirname, XrdCl::MkDirFlags::MakePath, mode, timeout);
	if (EOS_debug) {
	    printf("mkdir handle %d '%s' mode 0%o status = 0x%x %s\n", handle, dirname, mode, status.ToString().c_str());
	}
	env->ReleaseStringUTFChars(dirname_p, dirname);

	return *(jlong *) &status;

};

/*
 * Class:     ch_cern_eos_EOSFileSystem
 * Method:    listFileStatusS
 * Signature: (JLjava/lang/String;Lorg/apache/hadoop/fs/Path;)[Lorg/apache/hadoop/fs/FileStatus;
 */
JNIEXPORT jobjectArray JNICALL Java_ch_cern_eos_EOSFileSystem_listFileStatusS
  (JNIEnv *env, jobject This, jlong handle, jstring url_p, jobject pp) {

	XrdCl::FileSystem *fs = (XrdCl::FileSystem *) handle;
	const char *fn = env->GetStringUTFChars(url_p, 0);
	if (EOS_debug) {
	    std::cout << "listFileStatusS fs handle " << handle << "\n";
	    printf("listFileStatusS: '%s'\n", fn);
	}

	uint16_t timeout = 0;
	XrdCl::DirListFlags::Flags flags = XrdCl::DirListFlags::Stat;
	XrdCl::DirectoryList *list = NULL;
	XrdCl::XRootDStatus status = fs->DirList(fn, flags, list, timeout);

	env->ReleaseStringUTFChars(url_p, fn);

	int numEntries;
	if (list && status.status == 0)
	    numEntries = list->GetSize();
	else
	    numEntries = 0;

	if (EOS_debug) printf("listFileStatusS: found %d entries\n", numEntries);


	jclass cls_FileStatus = env->FindClass("org/apache/hadoop/fs/FileStatus");
	jobjectArray FSarray = env->NewObjectArray(numEntries, cls_FileStatus, NULL);

	jclass cls_Path = env->FindClass("org/apache/hadoop/fs/Path");
	jmethodID mid_FSinit = env->GetMethodID(cls_FileStatus, "<init>", "(JZIJJLorg/apache/hadoop/fs/Path;)V");		/* void FileStatus(long, boolean, int, long, long, Path) */
	jmethodID mid_Pathinit = env->GetMethodID(cls_Path, "<init>", "(Lorg/apache/hadoop/fs/Path;Ljava/lang/String;)V");	/* void Path(Path, String) */

	for (int i=0; i < numEntries; i++) {
	    XrdCl::StatInfo *si = list->At(i)->GetStatInfo();
	    std::string name = list->At(i)->GetName();
	    if (EOS_debug) printf("listFileStatusS entry %d: %s\n", i, name.c_str());

	    jstring j_name = env->NewStringUTF(name.c_str());
	    jobject path = env->NewObject(cls_Path, mid_Pathinit, pp, j_name);

	    jobject st = env->NewObject(cls_FileStatus, mid_FSinit, si->GetSize(), si->TestFlags(si->IsDir), 1, 256*1024L, si->GetModTime()*1000, path);
	    env->SetObjectArrayElement(FSarray, i, st);

	    env->DeleteLocalRef(j_name);
	    env->DeleteLocalRef(path);
	    env->DeleteLocalRef(st);
	}

	return FSarray;


};

/*
 * Class:     ch_cern_eos_EOSFileSystem
 * Method:    setcc
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ch_cern_eos_EOSFileSystem_setcc (JNIEnv *env, jclass This, jstring ccn_p) {
	const char *ccn = env->GetStringUTFChars(ccn_p, 0);

	int code = putenv((char *) ccn);
    
#if 0
	std::cout << "putenv " << ccn << " code=" << code << "\n";

	/*env->ReleaseStringUTFChars(ccn_p, ccn);*/

	system("/usr/bin/env");
	system("/usr/bin/klist");

	FILE *resp = popen("/usr/bin/klist 2>&1", "r");
	char buf[4096];
	while (1) {
	    int sz = fread(buf, 1, 4095, resp);
	    std::cout << "popen(klist) read sz=" << sz << "\n";
	    if (sz <= 0) break;
	    buf[sz] = '\0';
	    printf("%s", buf);
	}
	pclose(resp);
#endif

};

/*
 * Class:     ch_cern_eos_EOSFileSystem
 * Method:    getErrText
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_ch_cern_eos_EOSFileSystem_getErrText (JNIEnv *env, jobject This, jlong errcode) {
  XrdCl::XRootDStatus status;

  *(long *) &status = errcode;

  jstring msg = env->NewStringUTF(status.ToString().c_str());
  return msg;
};
#ifdef __cplusplus
}
#endif
