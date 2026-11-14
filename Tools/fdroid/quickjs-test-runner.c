// SPDX-License-Identifier: GPL-3.0-or-later
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "quickjs.h"

int main(int argc, char **argv) {
    if (argc != 2) return 2;
    FILE *file = fopen(argv[1], "rb");
    if (!file) return 2;
    fseek(file, 0, SEEK_END);
    long length = ftell(file);
    rewind(file);
    if (length < 0) return 2;
    char *source = malloc((size_t)length + 1);
    if (!source || fread(source, 1, (size_t)length, file) != (size_t)length) return 2;
    fclose(file);
    source[length] = 0;
    JSRuntime *runtime = JS_NewRuntime();
    JSContext *context = JS_NewContext(runtime);
    JSValue result = JS_Eval(context, source, (size_t)length, "builtin-tests.js", JS_EVAL_TYPE_GLOBAL);
    free(source);
    if (JS_IsException(result)) return 1;
    JS_FreeValue(context, result);
    JSContext *job_context;
    int jobs = 0, status;
    while ((status = JS_ExecutePendingJob(runtime, &job_context)) > 0 && ++jobs < 1000) {}
    if (status < 0 || jobs >= 1000) return 1;
    const char *check = "globalThis.ayulessBuiltinTestsPassed === true ? 'ok' : String(globalThis.ayulessBuiltinTestError)";
    result = JS_Eval(context, check, strlen(check), "result.js", JS_EVAL_TYPE_GLOBAL);
    const char *message = JS_ToCString(context, result);
    int failed = !message || strcmp(message, "ok") != 0;
    fprintf(failed ? stderr : stdout, "%s\n", message ? message : "test failed");
    JS_FreeCString(context, message);
    JS_FreeValue(context, result);
    JS_FreeContext(context);
    JS_FreeRuntime(runtime);
    return failed;
}
