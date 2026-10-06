// 在本机无法派生子进程（dart run / flutter CLI 都会 spawn）的环境里，
// 用 package:analyzer 直接在当前进程内做一次静态检查，等价于 `flutter analyze`。
//
// 用法：dart --packages=.dart_tool/package_config.json tool/analyze.dart [目录]

import 'dart:io';

import 'package:analyzer/dart/analysis/analysis_context.dart';
import 'package:analyzer/dart/analysis/analysis_context_collection.dart';
import 'package:analyzer/dart/analysis/results.dart';
import 'package:analyzer/error/error.dart';
import 'package:analyzer/file_system/physical_file_system.dart';
import 'package:path/path.dart' as p;

const List<String> _skipPatterns = [
  '.dart_tool',
  '.pub-cache',
  'generated_plugin_registrant',
];

Future<void> main(List<String> args) async {
  final root = Directory.current.path;
  final target = args.isNotEmpty ? args.first : 'lib';
  final included = <String>[p.normalize(p.absolute(root, target))];

  final collection = AnalysisContextCollection(
    includedPaths: included,
    resourceProvider: PhysicalResourceProvider.INSTANCE,
  );

  var errorCount = 0;
  var warningCount = 0;
  var infoCount = 0;

  for (final AnalysisContext context in collection.contexts) {
    for (final String path in context.contextRoot.analyzedFiles()) {
      if (!path.endsWith('.dart')) continue;
      if (_skipPatterns.any(path.contains)) continue;

      final SomeResolvedUnitResult result =
          await context.currentSession.getResolvedUnit(path);
      if (result is! ResolvedUnitResult) continue;
      for (final AnalysisError error in result.errors) {
        final severity = error.errorCode.errorSeverity.name;
        final location =
            '${error.source.uri.path}:${error.offset}:${error.offset + error.length}';
        final line = '[$severity] ${error.errorCode.name} $location\n    ${error.message}';
        switch (severity) {
          case 'ERROR':
            errorCount++;
            stdout.writeln(line);
            break;
          case 'WARNING':
            warningCount++;
            stdout.writeln(line);
            break;
          default:
            infoCount++;
            if (args.contains('--info')) stdout.writeln(line);
        }
      }
    }
  }

  stdout.writeln('---');
  stdout.writeln(
      'errors: $errorCount, warnings: $warningCount, infos/lints: $infoCount');
  if (errorCount > 0) exitCode = 1;
}
