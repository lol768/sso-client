# Autogenerated from a Treetop grammar. Edits may be lost.


module Cucumber
  module Parser
    # TIP: When you hack on the grammar, just delete py_string.rb in this directory.
    # Also make sure you have uninstalled all cucumber gems (don't forget xxx-cucumber
    # github gems).
    #
    # Treetop will then generate the parser in-memory. When you're happy, just generate
    # the rb file with tt feature.tt
    module PyString
      include Treetop::Runtime

      def root
        @root || :py_string
      end

      include Common

      module PyString0
      end

      module PyString1
        def open_py_string
          elements[0]
        end

        def s
          elements[1]
        end

        def close_py_string
          elements[2]
        end
      end

      module PyString2
        def at_line?(line)
          line >= open_py_string.line && line <= close_py_string.line
        end

        def build(filter=nil)
          Ast::PyString.new(open_py_string.line, close_py_string.line, s.text_value, open_py_string.indentation)
        end
      end

      def _nt_py_string
        start_index = index
        if node_cache[:py_string].has_key?(index)
          cached = node_cache[:py_string][index]
          if cached
            cached = SyntaxNode.new(input, index...(index + 1)) if cached == true
            @index = cached.interval.end
          end
          return cached
        end

        i0, s0 = index, []
        r1 = _nt_open_py_string
        s0 << r1
        if r1
          s2, i2 = [], index
          loop do
            i3, s3 = index, []
            i4 = index
            r5 = _nt_close_py_string
            if r5
              r4 = nil
            else
              @index = i4
              r4 = instantiate_node(SyntaxNode,input, index...index)
            end
            s3 << r4
            if r4
              if index < input_length
                r6 = instantiate_node(SyntaxNode,input, index...(index + 1))
                @index += 1
              else
                terminal_parse_failure("any character")
                r6 = nil
              end
              s3 << r6
            end
            if s3.last
              r3 = instantiate_node(SyntaxNode,input, i3...index, s3)
              r3.extend(PyString0)
            else
              @index = i3
              r3 = nil
            end
            if r3
              s2 << r3
            else
              break
            end
          end
          r2 = instantiate_node(SyntaxNode,input, i2...index, s2)
          s0 << r2
          if r2
            r7 = _nt_close_py_string
            s0 << r7
          end
        end
        if s0.last
          r0 = instantiate_node(SyntaxNode,input, i0...index, s0)
          r0.extend(PyString1)
          r0.extend(PyString2)
        else
          @index = i0
          r0 = nil
        end

        node_cache[:py_string][start_index] = r0

        r0
      end

      module OpenPyString0
        def indent
          elements[0]
        end

        def eol
          elements[3]
        end
      end

      module OpenPyString1
        def indentation
          indent.text_value.length
        end

        def line
          indent.line
        end
      end

      def _nt_open_py_string
        start_index = index
        if node_cache[:open_py_string].has_key?(index)
          cached = node_cache[:open_py_string][index]
          if cached
            cached = SyntaxNode.new(input, index...(index + 1)) if cached == true
            @index = cached.interval.end
          end
          return cached
        end

        i0, s0 = index, []
        s1, i1 = [], index
        loop do
          r2 = _nt_space
          if r2
            s1 << r2
          else
            break
          end
        end
        r1 = instantiate_node(SyntaxNode,input, i1...index, s1)
        s0 << r1
        if r1
          if has_terminal?('"""', false, index)
            r3 = instantiate_node(SyntaxNode,input, index...(index + 3))
            @index += 3
          else
            terminal_parse_failure('"""')
            r3 = nil
          end
          s0 << r3
          if r3
            s4, i4 = [], index
            loop do
              r5 = _nt_space
              if r5
                s4 << r5
              else
                break
              end
            end
            r4 = instantiate_node(SyntaxNode,input, i4...index, s4)
            s0 << r4
            if r4
              r6 = _nt_eol
              s0 << r6
            end
          end
        end
        if s0.last
          r0 = instantiate_node(SyntaxNode,input, i0...index, s0)
          r0.extend(OpenPyString0)
          r0.extend(OpenPyString1)
        else
          @index = i0
          r0 = nil
        end

        node_cache[:open_py_string][start_index] = r0

        r0
      end

      module ClosePyString0
        def eol
          elements[0]
        end

        def quotes
          elements[2]
        end

        def white
          elements[3]
        end
      end

      module ClosePyString1
        def line
          quotes.line
        end
      end

      def _nt_close_py_string
        start_index = index
        if node_cache[:close_py_string].has_key?(index)
          cached = node_cache[:close_py_string][index]
          if cached
            cached = SyntaxNode.new(input, index...(index + 1)) if cached == true
            @index = cached.interval.end
          end
          return cached
        end

        i0, s0 = index, []
        r1 = _nt_eol
        s0 << r1
        if r1
          s2, i2 = [], index
          loop do
            r3 = _nt_space
            if r3
              s2 << r3
            else
              break
            end
          end
          r2 = instantiate_node(SyntaxNode,input, i2...index, s2)
          s0 << r2
          if r2
            if has_terminal?('"""', false, index)
              r4 = instantiate_node(SyntaxNode,input, index...(index + 3))
              @index += 3
            else
              terminal_parse_failure('"""')
              r4 = nil
            end
            s0 << r4
            if r4
              r5 = _nt_white
              s0 << r5
            end
          end
        end
        if s0.last
          r0 = instantiate_node(SyntaxNode,input, i0...index, s0)
          r0.extend(ClosePyString0)
          r0.extend(ClosePyString1)
        else
          @index = i0
          r0 = nil
        end

        node_cache[:close_py_string][start_index] = r0

        r0
      end

    end

    class PyStringParser < Treetop::Runtime::CompiledParser
      include PyString
    end

  end
end